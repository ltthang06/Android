package com.example.loginapp;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class ShoppingFilters {
    private ShoppingFilters() {}

    public static boolean priceMatches(long value, long min, long max) {
        return value >= min && value <= max;
    }

    public static long dayStart(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, day);
        return calendar.getTimeInMillis();
    }

    public static int daysInMonth(int year, int month) {
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(year, month, 1);
        return date.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static boolean orderMatches(String id, String query, Date date, long from, long to) {
        if (!id.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT)))
            return false;
        if (from < 0 && to < 0) return true;
        if (date == null || (from >= 0 && date.getTime() < from)) return false;
        if (to >= 0) {
            Calendar end = Calendar.getInstance();
            end.setTimeInMillis(to);
            end.add(Calendar.DAY_OF_MONTH, 1);
            if (date.getTime() >= end.getTimeInMillis()) return false;
        }
        return true;
    }
}
