package com.example.loginapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {
    private EditText name, phone;
    private Button save;
    private String savedName = "", savedPhone = "";
    private boolean loaded, saving;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_profile);
        ScreenInsets.apply(this);
        findViewById(R.id.profileBanner).setClipToOutline(true);
        findViewById(R.id.btnProfileBack).setOnClickListener(v -> finish());
        FirebaseUser current = AccountAuth.auth().getCurrentUser();
        String id = getSharedPreferences("session", MODE_PRIVATE).getString("userId", "");
        if (current == null || !current.isEmailVerified() || id.isEmpty()) {
            finish();
            return;
        }
        name = findViewById(R.id.edtProfileUsername);
        phone = findViewById(R.id.edtProfilePhone);
        EditText email = findViewById(R.id.edtProfileEmail);
        save = findViewById(R.id.btnProfileUpdate);
        email.setText(current.getEmail());
        email.setKeyListener(null);
        email.setFocusable(false);
        name.setEnabled(false);
        phone.setEnabled(false);
        DocumentReference account =
                FirebaseFirestore.getInstance().collection("users").document(id);
        TextWatcher watcher =
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        updateSave();
                    }

                    @Override
                    public void afterTextChanged(Editable value) {}
                };
        name.addTextChangedListener(watcher);
        phone.addTextChangedListener(watcher);
        account.get()
                .addOnSuccessListener(
                        profile -> {
                            if (isDestroyed()) return;
                            if (!profile.exists()) {
                                finish();
                                return;
                            }
                            savedName =
                                    profile.getString("username") == null
                                            ? ""
                                            : profile.getString("username").trim();
                            savedPhone =
                                    profile.getString("phone") == null
                                            ? ""
                                            : profile.getString("phone").trim();
                            name.setText(
                                    state != null
                                            ? state.getString("draftName", savedName)
                                            : savedName);
                            phone.setText(
                                    state != null
                                            ? state.getString("draftPhone", savedPhone)
                                            : savedPhone);
                            loaded = true;
                            setSaving(false);
                        })
                .addOnFailureListener(
                        e -> {
                            Toast.makeText(this, AccountAuth.message(e), Toast.LENGTH_LONG).show();
                            finish();
                        });
        save.setOnClickListener(
                v -> {
                    if (!loaded || saving || !changed()) return;
                    String username = name.getText().toString().trim(),
                            number = InputValidator.normalizePhone(phone.getText().toString());
                    if (!username.matches("[\\p{L}\\p{N}_.-]{3,32}")) {
                        InputValidator.error(name, getString(R.string.auth_username_error));
                        return;
                    }
                    InputValidator.error(name, null);
                    if (!InputValidator.checkContact(email, phone)) return;
                    setSaving(true);
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .whereEqualTo("username", username)
                            .get()
                            .continueWithTask(
                                    found -> {
                                        if (!found.isSuccessful()) throw found.getException();
                                        for (DocumentSnapshot other : found.getResult())
                                            if (!id.equals(other.getId()))
                                                throw new IllegalArgumentException(
                                                        getString(R.string.auth_username_taken));
                                        return account.update(
                                                "username", username, "phone", number);
                                    })
                            .addOnSuccessListener(
                                    done -> {
                                        getSharedPreferences("session", MODE_PRIVATE)
                                                .edit()
                                                .putString("currentUser", username)
                                                .apply();
                                        if (isDestroyed()) return;
                                        savedName = username;
                                        savedPhone = number;
                                        name.setText(username);
                                        phone.setText(number);
                                        setSaving(false);
                                        Toast.makeText(
                                                        this,
                                                        R.string.profile_saved,
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                    })
                            .addOnFailureListener(
                                    e -> {
                                        if (isDestroyed()) return;
                                        setSaving(false);
                                        Toast.makeText(
                                                        this,
                                                        AccountAuth.message(e),
                                                        Toast.LENGTH_LONG)
                                                .show();
                                    });
                });
    }

    private boolean changed() {
        return !savedName.equals(name.getText().toString().trim())
                || !savedPhone.equals(InputValidator.normalizePhone(phone.getText().toString()));
    }

    private void updateSave() {
        save.setEnabled(loaded && !saving && changed());
    }

    private void setSaving(boolean value) {
        saving = value;
        name.setEnabled(loaded && !value);
        phone.setEnabled(loaded && !value);
        save.setText(value ? R.string.label_dang_xu_ly : R.string.label_luu_thay_doi);
        updateSave();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        if (loaded && !saving) {
            out.putString("draftName", name.getText().toString());
            out.putString("draftPhone", phone.getText().toString());
        }
        super.onSaveInstanceState(out);
    }
}
