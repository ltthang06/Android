package com.example.loginapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public final class ProductCards {
    private ProductCards() {}

    public static View create(Context context, ViewGroup parent, Product product) {
        View card = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        bind(card, product);
        return card;
    }

    public static void bind(View card, Product product) {
        ImageView image = card.findViewById(R.id.ivProduct);
        image.setImageResource(product.getImage());
        image.setContentDescription(product.getName());
        ((TextView) card.findViewById(R.id.tvName)).setText(product.getName());
        ((TextView) card.findViewById(R.id.tvPrice)).setText(product.getPriceText());
    }
}
