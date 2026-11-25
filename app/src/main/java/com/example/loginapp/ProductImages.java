package com.example.loginapp;

import java.util.Locale;

public final class ProductImages {
    private ProductImages() {}

    public static int find(String name) {
        if (name == null) return R.drawable.images;
        switch (name.toLowerCase(Locale.ROOT)) {
            case "dell":
                return R.drawable.dell;
            case "ip15":
                return R.drawable.ip15;
            case "keychron":
                return R.drawable.keychron;
            case "lg":
                return R.drawable.lg;
            case "logi":
                return R.drawable.logi;
            case "nghesony":
                return R.drawable.nghesony;
            case "s24":
                return R.drawable.s24;
            default:
                return R.drawable.images;
        }
    }
}
