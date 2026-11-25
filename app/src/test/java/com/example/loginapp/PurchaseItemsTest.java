package com.example.loginapp;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

public class PurchaseItemsTest {
    @Test
    public void emptySelectionNeverMeansBuyEverything() {
        assertFalse(PurchaseItems.selected(Collections.emptyList(), "iphone_15"));
        assertTrue(PurchaseItems.selected(Arrays.asList("iphone_15"), "iphone_15"));
        assertFalse(PurchaseItems.selected(Arrays.asList("iphone_15"), "dell_xps_13"));
        assertTrue(PurchaseItems.selected(null, "dell_xps_13"));
    }

    @Test
    public void validatesItemsAndCalculatesQuantityTotalsAsLong() {
        Map<String, Object> item = new HashMap<>();
        item.put("name", "Laptop");
        item.put("price", 25_000_000L);
        item.put("quantity", 100L);
        assertTrue(PurchaseItems.valid(item));
        assertEquals(2_500_000_000L, PurchaseItems.lineTotal(item));
        item.put("quantity", 0L);
        assertFalse(PurchaseItems.valid(item));
        item.put("quantity", 1L);
        item.put("price", -1L);
        assertFalse(PurchaseItems.valid(item));
        item.put("price", 1L);
        item.put("name", " ");
        assertFalse(PurchaseItems.valid(item));
        assertFalse(PurchaseItems.valid(null));
    }
}
