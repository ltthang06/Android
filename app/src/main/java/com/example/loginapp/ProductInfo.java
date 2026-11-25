package com.example.loginapp;

import android.app.Activity;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.*;

public final class ProductInfo {
    private ProductInfo() {}

    public static Map<String, Object> fallback(String name) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String line : ProductSpecifications.details(name).split("\n")) {
            int colon = line.indexOf(':');
            if (colon > 0)
                values.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
        }
        return values;
    }

    public static void render(
            Activity screen, LinearLayout parent, Map<?, ?> values, String overview) {
        parent.removeAllViews();
        title(screen, parent, screen.getString(R.string.technical_title));
        List<String> keys = new ArrayList<>();
        for (Object key : values.keySet()) keys.add(String.valueOf(key));
        Collections.sort(keys);
        int index = 0;
        for (String key : keys) {
            LinearLayout row = new LinearLayout(screen);
            row.setPadding(dp(screen, 10), dp(screen, 12), dp(screen, 10), dp(screen, 12));
            if (index++ % 2 == 0) row.setBackgroundColor(0xFFF3F0F9);
            TextView label = label(screen, key.replace(" tham khảo", ""), 13, 0xFF665B76);
            TextView value = label(screen, String.valueOf(values.get(key)), 14, 0xFF262038);
            value.setTypeface(null, Typeface.BOLD);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 0.42f));
            row.addView(value, new LinearLayout.LayoutParams(0, -2, 0.58f));
            parent.addView(row);
        }
        TextView heading = title(screen, parent, screen.getString(R.string.description_title));
        heading.setPadding(0, dp(screen, 24), 0, dp(screen, 12));
        TextView description = label(screen, overview, 15, 0xFF60586B);
        description.setLineSpacing(dp(screen, 4), 1);
        parent.addView(description);
    }

    private static TextView title(Activity screen, LinearLayout parent, String text) {
        TextView heading = label(screen, text, 18, 0xFF292344);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0, 0, 0, dp(screen, 16));
        parent.addView(heading);
        return heading;
    }

    private static TextView label(Activity screen, String text, int size, int color) {
        TextView label = new TextView(screen);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(color);
        return label;
    }

    private static int dp(Activity screen, int value) {
        return Math.round(value * screen.getResources().getDisplayMetrics().density);
    }
}
