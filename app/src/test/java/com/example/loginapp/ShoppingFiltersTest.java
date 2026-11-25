package com.example.loginapp;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

public class ShoppingFiltersTest {
    @Test
    public void priceFilterIncludesBothBounds() {
        assertTrue(ShoppingFilters.priceMatches(20, 20, 25));
        assertTrue(ShoppingFilters.priceMatches(25, 20, 25));
        assertFalse(ShoppingFilters.priceMatches(26, 20, 25));
        assertFalse(ShoppingFilters.priceMatches(19, 20, 25));
    }

    @Test
    public void dateFilterIncludesEntireFinalDayAndMatchesPartialCode() {
        long start = ShoppingFilters.dayStart(2026, Calendar.NOVEMBER, 20);
        long end = ShoppingFilters.dayStart(2026, Calendar.NOVEMBER, 21);
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(end);
        next.add(Calendar.DAY_OF_MONTH, 1);
        assertTrue(ShoppingFilters.orderMatches("AbC123", "bc1", new Date(start), start, end));
        assertTrue(
                ShoppingFilters.orderMatches(
                        "AbC123", "ABC", new Date(next.getTimeInMillis() - 1), start, end));
        assertFalse(
                ShoppingFilters.orderMatches(
                        "AbC123", "abc", new Date(next.getTimeInMillis()), start, end));
        assertFalse(ShoppingFilters.orderMatches("AbC123", "abc", new Date(start - 1), start, end));
        assertFalse(ShoppingFilters.orderMatches("AbC123", "other", new Date(start), -1, -1));
        assertTrue(ShoppingFilters.orderMatches("AbC123", "  abc ", null, -1, -1));
        assertFalse(ShoppingFilters.orderMatches("AbC123", "abc", null, start, end));
    }

    @Test
    public void monthLengthsHandleLeapYears() {
        assertEquals(29, ShoppingFilters.daysInMonth(2024, Calendar.FEBRUARY));
        assertEquals(28, ShoppingFilters.daysInMonth(2026, Calendar.FEBRUARY));
        assertEquals(30, ShoppingFilters.daysInMonth(2026, Calendar.NOVEMBER));
        assertEquals(31, ShoppingFilters.daysInMonth(2026, Calendar.DECEMBER));
    }
}
