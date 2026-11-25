package com.example.loginapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private Button login;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!AppCompatDelegate.getApplicationLocales().toLanguageTags().equals("vi"))
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("vi"));
        setContentView(R.layout.activity_main);
        ScreenInsets.apply(this);
        findViewById(R.id.loginBanner).setClipToOutline(true);
        EditText username = findViewById(R.id.edtUsername),
                password = findViewById(R.id.edtPassword);
        login = findViewById(R.id.btnLogin);
        login.setOnClickListener(
                v -> {
                    String name = username.getText().toString().trim(),
                            pass = password.getText().toString();
                    if (name.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this, R.string.auth_missing, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    busy(true);
                    AccountAuth.signIn(name, pass)
                            .addOnSuccessListener(
                                    result -> {
                                        FirebaseUser user = result.getUser();
                                        password.setText("");
                                        if (user != null && user.isEmailVerified())
                                            AccountAuth.openStore(this)
                                                    .addOnFailureListener(this::failed);
                                        else {
                                            busy(false);
                                            startActivity(
                                                    EmailActionActivity.verifyIntent(this, false));
                                        }
                                    })
                            .addOnFailureListener(this::failed);
                });
        findViewById(R.id.btnForgotPassword)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, EmailActionActivity.class)));
        findViewById(R.id.btnGoRegister)
                .setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        restoreSession();
    }

    private void restoreSession() {
        FirebaseUser user = AccountAuth.auth().getCurrentUser();
        if (user == null || !user.isEmailVerified()) return;
        busy(true);
        user.getIdToken(false)
                .continueWithTask(
                        token -> {
                            if (!token.isSuccessful()) throw token.getException();
                            return AccountAuth.openStore(this);
                        })
                .addOnFailureListener(
                        error -> {
                            if (isDestroyed() || isFinishing()) return;
                            busy(false);
                            String code =
                                    error instanceof FirebaseAuthException
                                            ? ((FirebaseAuthException) error).getErrorCode()
                                            : "";
                            if (error instanceof FirebaseAuthInvalidUserException
                                    || code.equals("ERROR_USER_TOKEN_EXPIRED")
                                    || code.equals("ERROR_INVALID_USER_TOKEN")) {
                                clearSession();
                                Toast.makeText(
                                                this,
                                                R.string.auth_session_expired,
                                                Toast.LENGTH_LONG)
                                        .show();
                                return;
                            }

                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.auth_restore_failed)
                                    .setMessage(AccountAuth.message(error))
                                    .setPositiveButton(
                                            R.string.auth_retry,
                                            (dialog, which) -> restoreSession())
                                    .setNegativeButton(
                                            R.string.auth_other_account,
                                            (dialog, which) -> clearSession())
                                    .show();
                        });
    }

    private void clearSession() {
        AccountAuth.auth().signOut();
        getSharedPreferences("session", MODE_PRIVATE)
                .edit()
                .remove("currentUser")
                .remove("userId")
                .remove("secureAuth")
                .apply();
    }

    private void busy(boolean value) {
        for (int id :
                new int[] {
                    R.id.btnLogin,
                    R.id.btnGoRegister,
                    R.id.btnForgotPassword,
                    R.id.edtUsername,
                    R.id.edtPassword
                }) findViewById(id).setEnabled(!value);
        login.setText(value ? R.string.label_dang_xu_ly : R.string.label_dang_nhap);
    }

    private void failed(Exception error) {
        if (isDestroyed()) return;
        busy(false);
        Toast.makeText(this, AccountAuth.message(error), Toast.LENGTH_LONG).show();
    }
}
