package com.example.loginapp;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class ScreenInsets {
    private ScreenInsets() {}

    public static void apply(Activity activity) {
        View root = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        int left = root.getPaddingLeft(), top = root.getPaddingTop();
        int right = root.getPaddingRight(), bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {
                    Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    Insets keyboard = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
                    view.setPadding(
                            left + bars.left,
                            top + bars.top,
                            right + bars.right,
                            bottom + Math.max(bars.bottom, keyboard.bottom));
                    return windowInsets;
                });
        ViewCompat.requestApplyInsets(root);
    }
}
