package com.example.loginapp;

import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private Money() {}

    public static String format(long amount) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + "đ";
    }
}
