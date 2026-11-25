package com.example.loginapp;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

public final class ProductFilterState {
    private static final long[][] PRICES = {
        {0, Long.MAX_VALUE},
        {0, 4_999_999},
        {5_000_000, 9_999_999},
        {10_000_000, 19_999_999},
        {20_000_000, Long.MAX_VALUE}
    };
    public int category, sort, price, rating;
    public String brand = "";

    public ProductFilterState copy() {
        ProductFilterState copy = new ProductFilterState();
        copy.category = category;
        copy.sort = sort;
        copy.price = price;
        copy.rating = rating;
        copy.brand = brand;
        return copy;
    }

    public void reset() {
        category = 0;
        sort = 0;
        price = 0;
        rating = 0;
        brand = "";
    }

    public int activeCount() {
        int count = 0;
        if (category > 0) count++;
        if (sort > 0) count++;
        if (price > 0) count++;
        if (rating > 0) count++;
        if (!brand.isEmpty()) count++;
        return count;
    }

    public boolean matches(Product product, String query, Map<String, Double> averages) {
        String name = normalize(product.getName());
        boolean phone =
                name.contains("iphone") || name.contains("samsung") || name.contains("dien thoai");
        boolean laptop = name.contains("laptop");
        if (!name.contains(normalize(query.trim()))) return false;
        if (category == 1 && !phone) return false;
        if (category == 2 && !laptop) return false;
        if (category == 3 && (phone || laptop)) return false;
        if (!ShoppingFilters.priceMatches(product.getPrice(), PRICES[price][0], PRICES[price][1]))
            return false;
        if (!brand.isEmpty() && !brand.equals(brand(product.getName()))) return false;
        return rating == 0 || averages.getOrDefault(product.getId(), 0d) >= rating;
    }

    public static String brand(String name) {
        String value = normalize(name);
        if (value.contains("iphone") || value.contains("apple")) return "Apple";
        String[] brands = {"Samsung", "Dell", "Keychron", "LG", "Logitech", "Sony"};
        for (String brand : brands)
            if (value.contains(brand.toLowerCase(Locale.ROOT))) return brand;
        return "";
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }
}
