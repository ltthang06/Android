package com.example.loginapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

public class EmailActionActivity extends AppCompatActivity {
    private static final String EXTRA_VERIFY = "com.example.loginapp.VERIFY_EMAIL";
    private static final String EXTRA_SEND = "com.example.loginapp.SEND_VERIFICATION";
    private boolean verify, busy;
    private long resendAt;
    private CountDownTimer timer;
    private Button send, check;
    private TextView status;
    private EditText email;

    public static Intent verifyIntent(Context context, boolean sendNow) {
        return new Intent(context, EmailActionActivity.class)
                .putExtra(EXTRA_VERIFY, true)
                .putExtra(EXTRA_SEND, sendNow);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_email_action);
        ScreenInsets.apply(this);
        verify = getIntent().getBooleanExtra(EXTRA_VERIFY, false);
        email = findViewById(R.id.actionEmail);
        send = findViewById(R.id.actionSend);
        check = findViewById(R.id.actionCheck);
        status = findViewById(R.id.actionStatus);
        ((TextView) findViewById(R.id.actionTitle))
                .setText(verify ? R.string.auth_verify_title : R.string.auth_forgot);
        ((TextView) findViewById(R.id.actionSubtitle))
                .setText(verify ? R.string.auth_verify_hint : R.string.auth_reset_hint);
        check.setVisibility(verify ? View.VISIBLE : View.GONE);
        FirebaseUser user = AccountAuth.auth().getCurrentUser();
        if (verify) {
            if (user == null) {
                finish();
                return;
            }
            email.setText(user.getEmail());
            email.setKeyListener(null);
        }
        if (state != null) {
            resendAt = state.getLong("resendAt");
            status.setText(state.getCharSequence("status", ""));
        }
        send.setOnClickListener(v -> send());
        check.setOnClickListener(
                v -> {
                    FirebaseUser current = AccountAuth.auth().getCurrentUser();
                    if (current == null) {
                        finish();
                        return;
                    }
                    setBusy(true);
                    current.reload()
                            .continueWithTask(
                                    reloaded -> {
                                        if (!reloaded.isSuccessful()) throw reloaded.getException();
                                        FirebaseUser fresh = AccountAuth.auth().getCurrentUser();
                                        if (fresh == null || !fresh.isEmailVerified())
                                            throw new IllegalStateException(
                                                    getString(R.string.auth_not_verified));
                                        return fresh.getIdToken(true);
                                    })
                            .continueWithTask(
                                    token -> {
                                        if (!token.isSuccessful()) throw token.getException();
                                        return AccountAuth.openStore(this);
                                    })
                            .addOnFailureListener(this::failed);
                });
        findViewById(R.id.actionBack)
                .setOnClickListener(
                        v -> {
                            if (verify) AccountAuth.auth().signOut();
                            startActivity(
                                    new Intent(this, MainActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            finish();
                        });
        tick();
        if (verify && state == null && getIntent().getBooleanExtra(EXTRA_SEND, false)) send();
    }

    private void send() {
        if (busy || System.currentTimeMillis() < resendAt) return;
        String address = AccountAuth.email(email.getText().toString());
        if (!Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
            InputValidator.error(email, getString(R.string.auth_email_error));
            return;
        }
        InputValidator.error(email, null);
        setBusy(true);
        Task<Void> task;
        if (verify) {
            FirebaseUser user = AccountAuth.auth().getCurrentUser();
            if (user == null) {
                finish();
                return;
            }
            task = user.sendEmailVerification();
        } else task = AccountAuth.auth().sendPasswordResetEmail(address);
        task.addOnSuccessListener(
                        done -> {
                            if (isDestroyed()) return;
                            resendAt = System.currentTimeMillis() + 60000;
                            status.setText(
                                    verify
                                            ? R.string.auth_verification_sent
                                            : R.string.auth_reset_sent);
                            setBusy(false);
                        })
                .addOnFailureListener(
                        error -> {
                            if (!verify
                                    && error instanceof FirebaseAuthException
                                    && "ERROR_USER_NOT_FOUND"
                                            .equals(
                                                    ((FirebaseAuthException) error)
                                                            .getErrorCode())) {
                                resendAt = System.currentTimeMillis() + 60000;
                                status.setText(R.string.auth_reset_sent);
                                setBusy(false);
                            } else failed(error);
                        });
    }

    private void failed(Exception error) {
        if (isDestroyed()) return;
        setBusy(false);
        status.setText(AccountAuth.message(error));
    }

    private void setBusy(boolean value) {
        busy = value;
        check.setEnabled(!value);
        tick();
    }

    private void tick() {
        if (timer != null) timer.cancel();
        long remaining = resendAt - System.currentTimeMillis();
        send.setEnabled(!busy && remaining <= 0);
        if (busy) {
            send.setText(R.string.label_dang_xu_ly);
        } else if (remaining > 0) {
            send.setText(countdown(remaining));
        } else {
            send.setText(verify ? R.string.auth_send_verification : R.string.auth_send_reset);
        }
        if (remaining > 0)
            timer =
                    new CountDownTimer(remaining, 1000) {
                        public void onTick(long ms) {
                            send.setText(countdown(ms));
                        }

                        public void onFinish() {
                            tick();
                        }
                    }.start();
    }

    private String countdown(long ms) {
        int seconds = (int) ((ms + 999) / 1000);
        return getResources().getQuantityString(R.plurals.auth_resend_seconds, seconds, seconds);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putLong("resendAt", resendAt);
        out.putCharSequence("status", status.getText());
        super.onSaveInstanceState(out);
    }

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
