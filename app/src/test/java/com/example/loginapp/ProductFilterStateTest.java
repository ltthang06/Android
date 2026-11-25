package com.example.loginapp;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

public class ProductFilterStateTest {
    private final Product iphone = new Product("iphone_15", "iPhone 15", 22_000_000, 0, "");
    private final Product samsung =
            new Product("samsung_s24", "Samsung Galaxy S24", 20_000_000, 0, "");

    @Test
    public void combinesCategoryBrandPriceSearchAndRating() {
        ProductFilterState filter = new ProductFilterState();
        filter.category = 1;
        filter.price = 4;
        filter.brand = "Apple";
        filter.rating = 4;
        Map<String, Double> ratings = Collections.singletonMap("iphone_15", 4.5);
        assertTrue(filter.matches(iphone, "IPHONE", ratings));
        assertFalse(filter.matches(samsung, "", ratings));
        assertFalse(filter.matches(iphone, "laptop", ratings));
        filter.category = 2;
        assertFalse(filter.matches(iphone, "", ratings));
        filter.category = 1;
        filter.price = 3;
        assertFalse(filter.matches(iphone, "", ratings));
    }

    @Test
    public void unratedProductsOnlyPassWithoutStarRestriction() {
        ProductFilterState filter = new ProductFilterState();
        assertTrue(filter.matches(iphone, "", Collections.emptyMap()));
        filter.rating = 1;
        assertFalse(filter.matches(iphone, "", Collections.emptyMap()));
        filter.rating = 4;
        assertFalse(filter.matches(iphone, "", Collections.singletonMap("iphone_15", 3.9)));
        assertTrue(filter.matches(iphone, "", Collections.singletonMap("iphone_15", 4d)));
    }

    @Test
    public void draftDoesNotChangeAppliedFilters() {
        ProductFilterState original = new ProductFilterState();
        ProductFilterState draft = original.copy();
        draft.brand = "Apple";
        draft.sort = 1;
        assertEquals(0, original.activeCount());
        assertEquals(2, draft.activeCount());
        assertEquals("Apple", ProductFilterState.brand("iPhone 15"));
        assertEquals("Logitech", ProductFilterState.brand("Chuột Logitech MX Master"));
    }

    @Test
    public void resetDraftKeepsAppliedFilters() {
        ProductFilterState applied = new ProductFilterState();
        applied.category = 1;
        applied.price = 4;
        applied.brand = "Apple";
        applied.rating = 4;
        applied.sort = 1;
        ProductFilterState draft = applied.copy();
        draft.reset();
        assertEquals(0, draft.activeCount());
        assertEquals(5, applied.activeCount());
        assertTrue(draft.matches(samsung, "", Collections.emptyMap()));
    }
}
