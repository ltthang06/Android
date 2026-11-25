package com.example.loginapp;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public final class OrderCards {
    private OrderCards() {}

    public static View add(
            Activity screen,
            LinearLayout parent,
            String status,
            String title,
            String meta,
            String items,
            String total,
            String action) {
        View card = screen.getLayoutInflater().inflate(R.layout.item_order_summary, parent, false);
        set(card, R.id.orderStatus, status);
        set(card, R.id.orderCardTitle, title);
        set(card, R.id.orderCardMeta, meta);
        set(card, R.id.orderCardItems, items);
        set(card, R.id.orderCardTotal, total);
        set(card, R.id.orderCardAction, action);
        card.findViewById(R.id.orderCardDivider)
                .setVisibility(total == null || total.isEmpty() ? View.GONE : View.VISIBLE);
        if (status == null || status.isEmpty()) {
            LinearLayout.LayoutParams titleParams =
                    (LinearLayout.LayoutParams)
                            card.findViewById(R.id.orderCardTitle).getLayoutParams();
            titleParams.topMargin = 0;
        }
        parent.addView(card);
        return card;
    }

    public static void thumbnails(Activity screen, View card, Object items) {
        if (!(items instanceof List<?>)) return;
        LinearLayout rail = card.findViewById(R.id.orderCardThumbnails);
        int size = Math.round(60 * screen.getResources().getDisplayMetrics().density);
        for (Object entry : (List<?>) items) {
            if (!(entry instanceof Map<?, ?>) || rail.getChildCount() == 3) continue;
            Map<?, ?> item = (Map<?, ?>) entry;
            ImageView image = new ImageView(screen);
            image.setImageResource(ProductImages.find(String.valueOf(item.get("image"))));
            image.setContentDescription(String.valueOf(item.get("name")));
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setBackgroundResource(R.drawable.bg_product_image);
            int padding = size / 8;
            image.setPadding(padding, padding, padding, padding);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(Math.round(10 * screen.getResources().getDisplayMetrics().density));
            rail.addView(image, params);
        }
        if (rail.getChildCount() > 0) rail.setVisibility(View.VISIBLE);
    }

    private static void set(View card, int id, String value) {
        TextView label = card.findViewById(id);
        label.setText(value);
        label.setVisibility(value == null || value.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
