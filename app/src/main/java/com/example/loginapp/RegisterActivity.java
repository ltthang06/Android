package com.example.loginapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private Button create;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_register);
        ScreenInsets.apply(this);
        findViewById(R.id.registerBanner).setClipToOutline(true);
        EditText name = findViewById(R.id.edtRegUsername), email = findViewById(R.id.edtRegEmail);
        EditText phone = findViewById(R.id.edtRegPhone),
                password = findViewById(R.id.edtRegPassword);
        EditText confirm = findViewById(R.id.edtRegConfirm);
        create = findViewById(R.id.btnRegister);
        create.setOnClickListener(
                v -> {
                    String username = name.getText().toString().trim(),
                            address = AccountAuth.email(email.getText().toString());
                    String pass = password.getText().toString(),
                            number = InputValidator.normalizePhone(phone.getText().toString());
                    if (!username.matches("[\\p{L}\\p{N}_.-]{3,32}")) {
                        InputValidator.error(name, getString(R.string.auth_username_error));
                        return;
                    }
                    InputValidator.error(name, null);
                    if (!InputValidator.checkContact(email, phone)) return;
                    if (pass.length() < 6 || pass.length() > 128) {
                        InputValidator.error(password, getString(R.string.auth_password_length));
                        return;
                    }
                    InputValidator.error(password, null);
                    if (!pass.equals(confirm.getText().toString())) {
                        InputValidator.error(confirm, getString(R.string.auth_password_mismatch));
                        return;
                    }
                    InputValidator.error(confirm, null);
                    busy(true);
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("users")
                            .whereEqualTo("username", username)
                            .limit(1)
                            .get()
                            .continueWithTask(
                                    names -> {
                                        if (!names.isSuccessful()) throw names.getException();
                                        if (!names.getResult().isEmpty())
                                            throw new IllegalArgumentException(
                                                    getString(R.string.auth_username_taken));
                                        return db.collection("users")
                                                .whereEqualTo("email", address)
                                                .limit(1)
                                                .get();
                                    })
                            .continueWithTask(
                                    emails -> {
                                        if (!emails.isSuccessful()) throw emails.getException();
                                        if (!emails.getResult().isEmpty())
                                            throw new IllegalArgumentException(
                                                    getString(R.string.auth_existing_email));
                                        return AccountAuth.auth()
                                                .createUserWithEmailAndPassword(address, pass);
                                    })
                            .addOnSuccessListener(
                                    result -> {
                                        FirebaseUser user = result.getUser();
                                        Map<String, Object> profile = new HashMap<>();
                                        profile.put("username", username);
                                        profile.put("email", address);
                                        profile.put("phone", number);
                                        profile.put("authUid", user.getUid());
                                        profile.put("authProvider", "email");
                                        user.updateProfile(
                                                        new UserProfileChangeRequest.Builder()
                                                                .setDisplayName(username)
                                                                .build())
                                                .continueWithTask(
                                                        updated -> {
                                                            if (!updated.isSuccessful())
                                                                throw updated.getException();
                                                            return db.collection("users")
                                                                    .document(user.getUid())
                                                                    .set(profile);
                                                        })
                                                .addOnSuccessListener(
                                                        saved -> {
                                                            password.setText("");
                                                            confirm.setText("");
                                                            startActivity(
                                                                    EmailActionActivity
                                                                            .verifyIntent(
                                                                                    this, true));
                                                            finish();
                                                        })
                                                .addOnFailureListener(
                                                        error -> {
                                                            new MaterialAlertDialogBuilder(this)
                                                                    .setTitle(
                                                                            R.string
                                                                                    .auth_account_created)
                                                                    .setMessage(
                                                                            R.string
                                                                                    .auth_profile_retry)
                                                                    .setPositiveButton(
                                                                            R.string.auth_continue,
                                                                            (d, w) -> {
                                                                                startActivity(
                                                                                        EmailActionActivity
                                                                                                .verifyIntent(
                                                                                                        this,
                                                                                                        true));
                                                                                finish();
                                                                            })
                                                                    .setCancelable(false)
                                                                    .show();
                                                        });
                                    })
                            .addOnFailureListener(
                                    error -> {
                                        busy(false);
                                        Toast.makeText(
                                                        this,
                                                        AccountAuth.message(error),
                                                        Toast.LENGTH_LONG)
                                                .show();
                                    });
                });
        findViewById(R.id.btnBackLogin).setOnClickListener(v -> finish());
    }

    private void busy(boolean value) {
        create.setEnabled(!value);
        create.setText(value ? R.string.label_dang_xu_ly : R.string.auth_create);
    }
}
