package com.example.loginapp;

import java.util.Collection;
import java.util.Map;

public final class PurchaseItems {
    private PurchaseItems() {}

    public static boolean valid(Map<?, ?> item) {
        return item != null
                && item.get("name") instanceof String
                && !((String) item.get("name")).trim().isEmpty()
                && item.get("price") instanceof Number
                && ((Number) item.get("price")).longValue() >= 0
                && item.get("quantity") instanceof Number
                && ((Number) item.get("quantity")).longValue() > 0;
    }

    public static boolean selected(Collection<String> ids, String id) {
        return ids == null || ids.contains(id);
    }

    public static long lineTotal(Map<?, ?> item) {
        return valid(item)
                ? ((Number) item.get("price")).longValue()
                        * ((Number) item.get("quantity")).longValue()
                : 0;
    }
}
