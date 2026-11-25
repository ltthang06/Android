package com.example.loginapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AccountAuth {
    private AccountAuth() {}

    public static FirebaseAuth auth() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.setLanguageCode("vi");
        return auth;
    }

    public static DocumentReference userDocument(Context context) {
        String id =
                context.getSharedPreferences("session", Context.MODE_PRIVATE)
                        .getString("userId", "");
        if (id.isEmpty()) throw new IllegalStateException("Vui lòng đăng nhập lại.");
        return FirebaseFirestore.getInstance().collection("users").document(id);
    }

    public static String currentEmail() {
        FirebaseUser user = auth().getCurrentUser();
        return user == null || user.getEmail() == null ? "" : user.getEmail();
    }

    public static String email(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static Task<AuthResult> signIn(String identifier, String password) {
        boolean isEmail = identifier.contains("@");
        return FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo(
                        isEmail ? "email" : "username", isEmail ? email(identifier) : identifier)
                .limit(2)
                .get()
                .continueWithTask(
                        found -> {
                            if (!found.isSuccessful()) throw found.getException();
                            if (found.getResult().size() > 1)
                                throw new IllegalStateException(
                                        "Thông tin tài khoản bị trùng. Vui lòng liên hệ người quản"
                                                + " lý.");
                            DocumentSnapshot old =
                                    found.getResult().isEmpty()
                                            ? null
                                            : found.getResult().getDocuments().get(0);
                            String address =
                                    isEmail
                                            ? email(identifier)
                                            : old == null ? "" : old.getString("email");
                            if (address == null || address.isEmpty())
                                throw new IllegalArgumentException(
                                        "Không tìm thấy email tài khoản. Vui lòng kiểm tra lại.");
                            return auth().signInWithEmailAndPassword(email(address), password)
                                    .continueWithTask(
                                            login -> {
                                                if (login.isSuccessful()) return login;

                                                Exception error = login.getException();
                                                String code =
                                                        error instanceof FirebaseAuthException
                                                                ? ((FirebaseAuthException) error)
                                                                        .getErrorCode()
                                                                : "";
                                                boolean invalidLogin =
                                                        code.equals("ERROR_USER_NOT_FOUND")
                                                                || code.equals(
                                                                        "ERROR_INVALID_CREDENTIAL")
                                                                || code.equals(
                                                                        "ERROR_WRONG_PASSWORD")
                                                                || code.equals(
                                                                        "ERROR_INVALID_LOGIN_CREDENTIALS");
                                                if (invalidLogin
                                                        && old != null
                                                        && !old.contains("authUid")
                                                        && password.equals(
                                                                old.getString("password")))
                                                    return auth().createUserWithEmailAndPassword(
                                                                    email(address), password);
                                                throw error;
                                            });
                        });
    }

    public static Task<DocumentSnapshot> profile() {
        FirebaseUser user = auth().getCurrentUser();
        if (user == null || !user.isEmailVerified())
            return Tasks.forException(
                    new IllegalStateException("Vui lòng xác minh email trước khi tiếp tục."));
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("users")
                .whereEqualTo("authUid", user.getUid())
                .limit(1)
                .get()
                .continueWithTask(
                        linked -> {
                            if (!linked.isSuccessful()) throw linked.getException();
                            if (!linked.getResult().isEmpty())
                                return Tasks.forResult(linked.getResult().getDocuments().get(0));
                            return db.collection("users")
                                    .whereEqualTo("email", user.getEmail())
                                    .limit(2)
                                    .get()
                                    .continueWithTask(
                                            found -> {
                                                if (!found.isSuccessful())
                                                    throw found.getException();
                                                if (found.getResult().size() > 1)
                                                    throw new IllegalStateException(
                                                            "Email đang gắn với nhiều hồ sơ. Vui"
                                                                + " lòng liên hệ người quản lý.");
                                                if (!found.getResult().isEmpty()) {
                                                    DocumentSnapshot old =
                                                            found.getResult().getDocuments().get(0);
                                                    if (old.contains("authUid")
                                                            && !user.getUid()
                                                                    .equals(
                                                                            old.getString(
                                                                                    "authUid")))
                                                        throw new IllegalStateException(
                                                                "Email đang liên kết với tài khoản"
                                                                        + " khác.");
                                                    return old.getReference()
                                                            .update(
                                                                    "authUid",
                                                                    user.getUid(),
                                                                    "authProvider",
                                                                    "email",
                                                                    "password",
                                                                    FieldValue.delete(),
                                                                    "authVersion",
                                                                    FieldValue.delete())
                                                            .continueWithTask(
                                                                    saved -> {
                                                                        if (!saved.isSuccessful())
                                                                            throw saved
                                                                                    .getException();
                                                                        return old.getReference()
                                                                                .get();
                                                                    });
                                                }
                                                Map<String, Object> data = new HashMap<>();
                                                data.put("authUid", user.getUid());
                                                data.put("authProvider", "email");
                                                data.put("email", user.getEmail());
                                                data.put("phone", "");
                                                data.put(
                                                        "username",
                                                        user.getDisplayName() == null
                                                                ? user.getEmail().split("@")[0]
                                                                : user.getDisplayName());
                                                return db.collection("users")
                                                        .document(user.getUid())
                                                        .set(data)
                                                        .continueWithTask(
                                                                saved -> {
                                                                    if (!saved.isSuccessful())
                                                                        throw saved.getException();
                                                                    return db.collection("users")
                                                                            .document(user.getUid())
                                                                            .get();
                                                                });
                                            });
                        });
    }

    public static Task<Void> openStore(Activity activity) {
        return profile()
                .continueWithTask(
                        task -> {
                            if (!task.isSuccessful()) throw task.getException();
                            DocumentSnapshot profile = task.getResult();
                            activity.runOnUiThread(
                                    () -> {
                                        if (activity.isDestroyed() || activity.isFinishing())
                                            return;
                                        activity.getSharedPreferences(
                                                        "session", Activity.MODE_PRIVATE)
                                                .edit()
                                                .putString(
                                                        "currentUser",
                                                        profile.getString("username"))
                                                .putString("userId", profile.getId())
                                                .remove("secureAuth")
                                                .apply();
                                        activity.startActivity(
                                                new Intent(activity, ProductActivity.class)
                                                        .addFlags(
                                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                                                        | Intent
                                                                                .FLAG_ACTIVITY_CLEAR_TASK));
                                        activity.finish();
                                    });
                            return Tasks.forResult(null);
                        });
    }

    public static String message(Exception error) {
        if (error.getMessage() != null && error.getMessage().contains("CONFIGURATION_NOT_FOUND"))
            return "Dự án Firebase chưa khởi tạo Authentication. Hãy bật phương thức"
                    + " Email/Password.";
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            switch (code) {
                case "ERROR_OPERATION_NOT_ALLOWED":
                case "ERROR_CONFIGURATION_NOT_FOUND":
                    return "Đăng nhập email chưa được bật trong Firebase Authentication.";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Email đã được đăng ký. Hãy đăng nhập hoặc chọn Quên mật khẩu.";
                case "ERROR_INVALID_EMAIL":
                    return "Email chưa đúng định dạng.";
                case "ERROR_WEAK_PASSWORD":
                    return "Mật khẩu cần ít nhất 6 ký tự.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Bạn thao tác quá nhiều. Vui lòng thử lại sau.";
                case "ERROR_USER_DISABLED":
                    return "Tài khoản đã bị vô hiệu hóa.";
                case "ERROR_USER_NOT_FOUND":
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_INVALID_CREDENTIAL":
                case "ERROR_INVALID_LOGIN_CREDENTIALS":
                    return "Email hoặc mật khẩu không đúng.";
                case "ERROR_REQUIRES_RECENT_LOGIN":
                    return "Vui lòng đăng nhập lại rồi thử lại.";
            }
        }
        if (error instanceof FirebaseNetworkException)
            return "Chưa kết nối được. Kiểm tra mạng rồi thử lại.";
        return error.getMessage() == null
                ? "Chưa thực hiện được. Vui lòng thử lại."
                : error.getMessage();
    }
}
