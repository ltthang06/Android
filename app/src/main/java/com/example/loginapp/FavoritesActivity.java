package com.example.loginapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;

import java.util.*;

public class FavoritesActivity extends AppCompatActivity {
    private LinearLayout content;
    private int request;
    private DocumentReference profile;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (getSharedPreferences("session", MODE_PRIVATE).getString("userId", "").isEmpty()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_order);
        ScreenInsets.apply(this);
        content = findViewById(R.id.orderContent);
        profile = AccountAuth.userDocument(this);
        findViewById(R.id.btnOrderBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) load();
    }

    private void load() {
        int token = ++request;
        content.removeAllViews();
        TextView title = new TextView(this);
        title.setText(R.string.favorites_title);
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getColor(R.color.text_primary));
        content.addView(title);
        TextView status = new TextView(this);
        status.setText(R.string.shopping_loading);
        content.addView(status);
        profile.collection("favorites")
                .get()
                .addOnSuccessListener(
                        favorites -> {
                            if (!current(token)) return;
                            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                            for (DocumentSnapshot favorite : favorites)
                                tasks.add(
                                        FirebaseFirestore.getInstance()
                                                .collection("products")
                                                .document(favorite.getId())
                                                .get());
                            Tasks.<DocumentSnapshot>whenAllSuccess(tasks)
                                    .addOnSuccessListener(
                                            products -> {
                                                if (!current(token)) return;
                                                for (DocumentSnapshot product : products) {
                                                    String name = product.getString("name");
                                                    Long price = product.getLong("price");
                                                    boolean available =
                                                            product.exists()
                                                                    && name != null
                                                                    && price != null;
                                                    View card =
                                                            getLayoutInflater()
                                                                    .inflate(
                                                                            R.layout.item_favorite,
                                                                            content,
                                                                            false);
                                                    content.addView(card);
                                                    TextView titleView =
                                                            card.findViewById(R.id.favoriteName);
                                                    TextView priceView =
                                                            card.findViewById(R.id.favoritePrice);
                                                    View open =
                                                            card.findViewById(R.id.favoriteOpen);
                                                    if (available) {
                                                        Product model =
                                                                new Product(
                                                                        product.getId(),
                                                                        name,
                                                                        price.intValue(),
                                                                        ProductImages.find(
                                                                                product.getString(
                                                                                        "image")),
                                                                        product.getString(
                                                                                "description"));
                                                        ((ImageView)
                                                                        card.findViewById(
                                                                                R.id.favoriteImage))
                                                                .setImageResource(model.getImage());
                                                        titleView.setText(name);
                                                        priceView.setText(Money.format(price));
                                                        open.setOnClickListener(
                                                                v ->
                                                                        startActivity(
                                                                                ProductDetailActivity
                                                                                        .createIntent(
                                                                                                this,
                                                                                                model)));
                                                    } else {
                                                        titleView.setText(
                                                                R.string.favorite_unavailable);
                                                        priceView.setVisibility(View.GONE);
                                                        card.findViewById(R.id.favoriteImage)
                                                                .setVisibility(View.GONE);
                                                        open.setClickable(false);
                                                        open.setFocusable(false);
                                                    }
                                                    ImageButton remove =
                                                            card.findViewById(R.id.favoriteRemove);
                                                    remove.setContentDescription(
                                                            getString(
                                                                    R.string.favorite_remove_named,
                                                                    available
                                                                            ? name
                                                                            : getString(
                                                                                    R.string
                                                                                            .favorite_unavailable_short)));
                                                    remove.setOnClickListener(
                                                            v -> {
                                                                remove.setEnabled(false);
                                                                profile.collection("favorites")
                                                                        .document(product.getId())
                                                                        .delete()
                                                                        .addOnSuccessListener(
                                                                                done -> load())
                                                                        .addOnFailureListener(
                                                                                e -> {
                                                                                    remove
                                                                                            .setEnabled(
                                                                                                    true);
                                                                                    error(e);
                                                                                });
                                                            });
                                                }
                                                status.setVisibility(View.GONE);
                                                if (products.isEmpty()) {
                                                    View empty =
                                                            getLayoutInflater()
                                                                    .inflate(
                                                                            R.layout
                                                                                    .empty_favorites,
                                                                            content,
                                                                            false);
                                                    content.addView(empty);
                                                    empty.findViewById(R.id.btnFavoriteBrowse)
                                                            .setOnClickListener(
                                                                    v -> {
                                                                        startActivity(
                                                                                new Intent(
                                                                                                this,
                                                                                                ProductActivity
                                                                                                        .class)
                                                                                        .addFlags(
                                                                                                Intent
                                                                                                                .FLAG_ACTIVITY_CLEAR_TOP
                                                                                                        | Intent
                                                                                                                .FLAG_ACTIVITY_SINGLE_TOP)
                                                                                        .putExtra(
                                                                                                "open_products",
                                                                                                true));
                                                                        finish();
                                                                    });
                                                }
                                            })
                                    .addOnFailureListener(
                                            e -> {
                                                if (current(token)) {
                                                    status.setText(
                                                            R.string.shopping_products_error);
                                                    error(e);
                                                }
                                            });
                        })
                .addOnFailureListener(
                        e -> {
                            if (current(token)) {
                                status.setText(R.string.favorite_load_error);
                                error(e);
                            }
                        });
    }

    private boolean current(int token) {
        return token == request && !isFinishing() && !isDestroyed();
    }

    private void error(Exception e) {
        Toast.makeText(this, AccountAuth.message(e), Toast.LENGTH_LONG).show();
    }
}
