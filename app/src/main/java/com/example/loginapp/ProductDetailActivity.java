package com.example.loginapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "product_id";
    public static final String EXTRA_NAME = "product_name";
    public static final String EXTRA_PRICE = "product_price";
    public static final String EXTRA_PRICE_VALUE = "product_priceValue";
    public static final String EXTRA_IMAGE = "product_image";
    public static final String EXTRA_DESCRIPTION = "product_description";

    public static Intent createIntent(Context context, Product product) {
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra(EXTRA_ID, product.getId());
        intent.putExtra(EXTRA_NAME, product.getName());
        intent.putExtra(EXTRA_PRICE, product.getPriceText());
        intent.putExtra(EXTRA_PRICE_VALUE, product.getPrice());
        intent.putExtra(EXTRA_IMAGE, product.getImage());
        intent.putExtra(EXTRA_DESCRIPTION, product.getDescription());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        ScreenInsets.apply(this);

        ImageView image = findViewById(R.id.ivDetailImage);
        TextView name = findViewById(R.id.tvDetailName);
        TextView price = findViewById(R.id.tvDetailPrice);

        String productId = getIntent().getStringExtra(EXTRA_ID);
        String productName = getIntent().getStringExtra(EXTRA_NAME);
        String productPrice = getIntent().getStringExtra(EXTRA_PRICE);
        String productDescription = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        int productImage = getIntent().getIntExtra(EXTRA_IMAGE, 0);

        if (productName == null || productName.trim().isEmpty()) {
            finish();
            return;
        }
        if (productImage == 0) productImage = R.drawable.images;
        final int imageResource = productImage;
        image.setImageResource(imageResource);
        name.setText(productName);
        price.setText(productPrice);
        LinearLayout specsTable = findViewById(R.id.specificationTable);
        String sampleDescription = ProductSpecifications.description(productName);
        String fallbackDescription =
                sampleDescription.isEmpty()
                        ? (productDescription == null ? "" : productDescription)
                        : sampleDescription;
        ProductInfo.render(
                this, specsTable, ProductInfo.fallback(productName), fallbackDescription);
        if (productId != null) {
            FirebaseFirestore.getInstance()
                    .collection("products")
                    .document(productId)
                    .get()
                    .addOnSuccessListener(
                            product -> {
                                Object specs = product.get("specifications");
                                String overview = product.getString("overview");
                                if (specs instanceof Map<?, ?>)
                                    ProductInfo.render(
                                            this,
                                            specsTable,
                                            (Map<?, ?>) specs,
                                            overview == null ? fallbackDescription : overview);
                            });
        }
        LinearLayout related = findViewById(R.id.relatedProducts);
        FirebaseFirestore.getInstance()
                .collection("products")
                .get()
                .addOnSuccessListener(
                        result -> {
                            if (isDestroyed() || isFinishing()) return;
                            for (DocumentSnapshot document : result) {
                                String otherName = document.getString("name");
                                Long otherPrice = document.getLong("price");
                                String imageName = document.getString("image");
                                if (otherName == null
                                        || otherName.equals(productName)
                                        || otherPrice == null
                                        || imageName == null) continue;
                                int otherImage = ProductImages.find(imageName);
                                Product other =
                                        new Product(
                                                document.getId(),
                                                otherName,
                                                otherPrice.intValue(),
                                                otherImage,
                                                document.getString("description"));
                                View row = ProductCards.create(this, related, other);
                                LinearLayout.LayoutParams params =
                                        new LinearLayout.LayoutParams(
                                                Math.round(
                                                        168
                                                                * getResources()
                                                                        .getDisplayMetrics()
                                                                        .density),
                                                -2);
                                params.setMarginEnd(
                                        Math.round(
                                                12 * getResources().getDisplayMetrics().density));
                                related.addView(row, params);
                                row.setOnClickListener(
                                        v -> {
                                            startActivity(createIntent(this, other));
                                            finish();
                                        });
                            }
                            if (related.getChildCount() > 0)
                                findViewById(R.id.tvRelatedTitle).setVisibility(View.VISIBLE);
                        });
        MaterialButton favorite = findViewById(R.id.btnFavorite);
        favorite.setEnabled(false);
        if (productId != null) {
            DocumentReference savedFavorite =
                    AccountAuth.userDocument(this).collection("favorites").document(productId);
            boolean[] saved = {false};
            savedFavorite
                    .get()
                    .addOnSuccessListener(
                            value -> {
                                if (isDestroyed() || isFinishing()) return;
                                saved[0] = value.exists();
                                renderFavorite(favorite, saved[0]);
                            })
                    .addOnFailureListener(
                            e ->
                                    favorite.setContentDescription(
                                            getString(R.string.favorite_state_error)));
            favorite.setOnClickListener(
                    v -> {
                        favorite.setEnabled(false);
                        Task<Void> update;
                        if (saved[0]) update = savedFavorite.delete();
                        else
                            update =
                                    savedFavorite.set(
                                            Collections.singletonMap(
                                                    "createdAt", FieldValue.serverTimestamp()));
                        update.addOnSuccessListener(
                                        done -> {
                                            if (isDestroyed() || isFinishing()) return;
                                            saved[0] = !saved[0];
                                            renderFavorite(favorite, saved[0]);
                                        })
                                .addOnFailureListener(
                                        e -> {
                                            favorite.setEnabled(true);
                                            Toast.makeText(
                                                            this,
                                                            "Không lưu được yêu thích. Vui lòng thử"
                                                                    + " lại.",
                                                            Toast.LENGTH_LONG)
                                                    .show();
                                        });
                    });
        }
        ProductReviews.attach(this, findViewById(R.id.reviewSection), productId, productName);
        Button btnBack = findViewById(R.id.btnBackToProducts);
        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnBuyNow)
                .setOnClickListener(
                        v -> {
                            Intent order = new Intent(this, OrderActivity.class);
                            order.putExtras(getIntent());
                            startActivity(order);
                        });
        Button btnAddToCart = findViewById(R.id.btnAddToCart);
        btnAddToCart.setOnClickListener(
                v -> {
                    SharedPreferences session =
                            getSharedPreferences("session", Context.MODE_PRIVATE);
                    String userId = session.getString("userId", "");
                    if (userId.isEmpty()) {
                        Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    CollectionReference cart =
                            db.collection("users").document(userId).collection("cart");
                    DocumentReference legacy = cart.document(Uri.encode(productName));
                    DocumentReference item =
                            cart.document(productId == null ? Uri.encode(productName) : productId);
                    btnAddToCart.setEnabled(false);
                    db.runTransaction(
                                    transaction -> {
                                        DocumentSnapshot saved = transaction.get(item);
                                        DocumentSnapshot old =
                                                item.getId().equals(legacy.getId())
                                                        ? null
                                                        : transaction.get(legacy);
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("productId", productId);
                                        data.put("name", productName);
                                        data.put(
                                                "price",
                                                getIntent().getIntExtra(EXTRA_PRICE_VALUE, 0));
                                        data.put(
                                                "image",
                                                getResources().getResourceEntryName(imageResource));
                                        long quantity =
                                                saved.getLong("quantity") == null
                                                        ? 0
                                                        : saved.getLong("quantity");
                                        if (old != null && old.getLong("quantity") != null)
                                            quantity += old.getLong("quantity");
                                        data.put("quantity", quantity + 1);
                                        transaction.set(item, data);
                                        if (old != null && old.exists()) transaction.delete(legacy);
                                        return null;
                                    })
                            .addOnSuccessListener(
                                    done -> {
                                        btnAddToCart.setEnabled(true);
                                        Toast.makeText(
                                                        this,
                                                        "Đã thêm vào giỏ hàng",
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                    })
                            .addOnFailureListener(
                                    error -> {
                                        btnAddToCart.setEnabled(true);
                                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG)
                                                .show();
                                    });
                });
    }

    private void renderFavorite(MaterialButton button, boolean saved) {
        button.setIconResource(saved ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite);
        button.setText(saved ? R.string.favorite_saved : R.string.favorite_action);
        button.setContentDescription(
                getString(saved ? R.string.favorite_remove : R.string.favorite_action));
        button.setEnabled(true);
    }
}
