package com.example.loginapp;

import android.app.Activity;
import android.content.Context;
import android.os.LocaleList;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public final class ProductReviews {
    private ProductReviews() {}

    public static void attach(
            Activity screen, LinearLayout parent, String productId, String productName) {
        if (productId == null || productId.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId =
                screen.getSharedPreferences("session", Context.MODE_PRIVATE)
                        .getString("userId", "");
        String username =
                screen.getSharedPreferences("session", Context.MODE_PRIVATE)
                        .getString("currentUser", "");
        CollectionReference reviews =
                db.collection("products").document(productId).collection("reviews");
        View summary = screen.getLayoutInflater().inflate(R.layout.review_summary, parent, false);
        parent.addView(summary);
        RatingBar stars = summary.findViewById(R.id.reviewAverageStars);
        TextView average = summary.findViewById(R.id.reviewAverageText);
        TextView score = summary.findViewById(R.id.reviewAverageScore);
        LinearLayout list = new LinearLayout(screen);
        list.setOrientation(LinearLayout.VERTICAL);
        parent.addView(list);
        View editor = screen.getLayoutInflater().inflate(R.layout.dialog_review, null);
        RatingBar input = editor.findViewById(R.id.reviewInputRating);
        EditText comment = editor.findViewById(R.id.reviewComment);
        comment.setImeHintLocales(LocaleList.forLanguageTags("vi-VN"));
        TextView ratingLabel = editor.findViewById(R.id.reviewRatingLabel);
        input.setOnRatingBarChangeListener(
                (bar, rating, fromUser) -> {
                    String[] labels = {
                        "Chọn từ 1 đến 5 sao",
                        "Rất không hài lòng",
                        "Chưa hài lòng",
                        "Bình thường",
                        "Hài lòng",
                        "Rất hài lòng"
                    };
                    ratingLabel.setText(labels[Math.max(0, Math.min(5, (int) rating))]);
                });
        MaterialButton send = editor.findViewById(R.id.reviewSave);
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(screen)
                        .setTitle("Chia sẻ trải nghiệm")
                        .setView(editor)
                        .setNegativeButton("Đóng", null)
                        .create();
        MaterialButton write = new MaterialButton(screen);
        write.setText(R.string.review_write);
        write.setMinHeight((int) Math.ceil(48 * screen.getResources().getDisplayMetrics().density));
        write.setInsetTop(0);
        write.setInsetBottom(0);
        parent.addView(write, new LinearLayout.LayoutParams(-1, -2));
        write.setOnClickListener(v -> dialog.show());
        write.setEnabled(false);
        if (userId.isEmpty()) write.setVisibility(View.GONE);
        Runnable load =
                () ->
                        reviews.get()
                                .addOnSuccessListener(
                                        result -> {
                                            if (screen.isDestroyed() || screen.isFinishing())
                                                return;
                                            list.removeAllViews();
                                            double total = 0;
                                            int count = 0;
                                            write.setText(R.string.review_write);
                                            for (DocumentSnapshot review : result) {
                                                Object value = review.get("rating");
                                                if (!(value instanceof Number)) continue;
                                                Number rating = (Number) value;
                                                if (rating.doubleValue() < 1
                                                        || rating.doubleValue() > 5) continue;
                                                count++;
                                                if (review.getId().equals(userId)) {
                                                    write.setText(R.string.review_edit);
                                                    input.setRating(rating.floatValue());
                                                    comment.setText(review.getString("comment"));
                                                }
                                                total += rating.doubleValue();
                                                View row =
                                                        screen.getLayoutInflater()
                                                                .inflate(
                                                                        R.layout.item_review,
                                                                        list,
                                                                        false);
                                                String reviewer = review.getString("username");
                                                if (reviewer == null || reviewer.isEmpty())
                                                    reviewer = "Khách hàng";
                                                ((TextView) row.findViewById(R.id.reviewerName))
                                                        .setText(reviewer);
                                                ((TextView) row.findViewById(R.id.reviewerAvatar))
                                                        .setText(
                                                                reviewer.substring(0, 1)
                                                                        .toUpperCase(Locale.ROOT));
                                                String date =
                                                        review.getDate("createdAt") == null
                                                                ? ""
                                                                : new SimpleDateFormat(
                                                                                "dd/MM/yyyy",
                                                                                Locale.getDefault())
                                                                        .format(
                                                                                review.getDate(
                                                                                        "createdAt"));
                                                ((TextView) row.findViewById(R.id.reviewDate))
                                                        .setText(
                                                                Boolean.TRUE.equals(
                                                                                review.getBoolean(
                                                                                        "ordered"))
                                                                        ? screen.getString(
                                                                                R.string
                                                                                        .review_date_ordered,
                                                                                date)
                                                                        : date);
                                                ((TextView) row.findViewById(R.id.reviewScore))
                                                        .setText(
                                                                screen.getString(
                                                                        R.string.review_score,
                                                                        rating.intValue()));
                                                ((TextView) row.findViewById(R.id.reviewBody))
                                                        .setText(review.getString("comment"));
                                                list.addView(row);
                                            }
                                            stars.setRating(
                                                    count == 0 ? 0 : (float) (total / count));
                                            score.setText(
                                                    count == 0
                                                            ? "—"
                                                            : String.format(
                                                                    Locale.getDefault(),
                                                                    "%.1f",
                                                                    total / count));
                                            average.setText(
                                                    count == 0
                                                            ? "Chưa có đánh giá. Hãy chia sẻ trải"
                                                                    + " nghiệm của bạn."
                                                            : screen.getString(
                                                                    R.string.review_count, count));
                                            write.setEnabled(true);
                                        })
                                .addOnFailureListener(
                                        error -> {
                                            write.setEnabled(false);
                                            average.setText(R.string.review_load_failed);
                                            Toast.makeText(
                                                            screen,
                                                            error.getMessage(),
                                                            Toast.LENGTH_LONG)
                                                    .show();
                                        });
        load.run();
        send.setOnClickListener(
                v -> {
                    String message = comment.getText().toString().trim();
                    if (input.getRating() < 1 || message.isEmpty()) {
                        InputValidator.error(comment, "Chọn số sao và nhập nhận xét");
                        return;
                    }
                    InputValidator.error(comment, null);
                    send.setEnabled(false);
                    db.collection("users")
                            .document(userId)
                            .collection("orders")
                            .get()
                            .addOnSuccessListener(
                                    orders -> {
                                        boolean ordered = false;
                                        for (DocumentSnapshot order : orders) {
                                            Object items = order.get("items");
                                            if (!(items instanceof List<?>)) continue;
                                            for (Object item : (List<?>) items) {
                                                if (!(item instanceof Map<?, ?>)) continue;
                                                Map<?, ?> product = (Map<?, ?>) item;
                                                if (productId.equals(product.get("productId"))
                                                        || productName.equals(
                                                                product.get("name"))) {
                                                    ordered = true;
                                                    break;
                                                }
                                            }
                                            if (ordered) break;
                                        }
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("username", username);
                                        data.put("rating", (int) input.getRating());
                                        data.put("comment", message);
                                        data.put("ordered", ordered);
                                        data.put("createdAt", FieldValue.serverTimestamp());
                                        reviews.document(userId)
                                                .set(data)
                                                .addOnSuccessListener(
                                                        done -> {
                                                            send.setEnabled(true);
                                                            dialog.dismiss();
                                                            load.run();
                                                            Toast.makeText(
                                                                            screen,
                                                                            "Đã lưu đánh giá",
                                                                            Toast.LENGTH_SHORT)
                                                                    .show();
                                                        })
                                                .addOnFailureListener(
                                                        error -> {
                                                            send.setEnabled(true);
                                                            Toast.makeText(
                                                                            screen,
                                                                            error.getMessage(),
                                                                            Toast.LENGTH_LONG)
                                                                    .show();
                                                        });
                                    })
                            .addOnFailureListener(
                                    error -> {
                                        send.setEnabled(true);
                                        Toast.makeText(
                                                        screen,
                                                        error.getMessage(),
                                                        Toast.LENGTH_LONG)
                                                .show();
                                    });
                });
    }
}
