package com.example.loginapp;

import static org.junit.Assert.*;

import org.junit.Test;

public class CheckoutValidationTest {
    @Test
    public void acceptsLocalAndInternationalMobileNumbers() {
        assertTrue(InputValidator.validPhone("0721123110"));
        assertTrue(InputValidator.validPhone("+84 721 123 110"));
        assertEquals("+84721123110", InputValidator.normalizePhone("+84 (721) 123-110"));
    }

    @Test
    public void rejectsInvalidMobileNumbers() {
        for (String value :
                new String[] {
                    "",
                    "1234567890",
                    "0123456789",
                    "072112311",
                    "07211231100",
                    "+840721123110",
                    "072abc3110"
                }) assertFalse(value, InputValidator.validPhone(value));
    }

    @Test
    public void formatsLargeOrderTotalsWithoutIntegerOverflow() {
        assertEquals("3.000.000.000đ", Money.format(3_000_000_000L));
        assertEquals("59.700.000đ", Money.format(59_700_000L));
        assertEquals("0đ", Money.format(0));
    }
}
