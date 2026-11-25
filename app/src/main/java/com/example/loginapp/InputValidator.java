package com.example.loginapp;

import android.util.Patterns;
import android.view.View;
import android.view.ViewParent;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

public final class InputValidator {
    private InputValidator() {}

    public static boolean checkContact(EditText emailField, EditText phoneField) {
        error(emailField, null);
        error(phoneField, null);
        String email = emailField.getText().toString().trim();
        String phone = normalizePhone(phoneField.getText().toString());
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            error(emailField, "Email không đúng định dạng!");
            emailField.requestFocus();
            return false;
        }
        if (!validPhone(phone)) {
            error(phoneField, "Nhập số điện thoại Việt Nam hợp lệ!");
            phoneField.requestFocus();
            return false;
        }
        return true;
    }

    public static boolean checkDelivery(EditText name, EditText phone, EditText address) {
        for (EditText field : new EditText[] {name, phone, address})
            InputValidator.error(field, null);
        EditText first = null;
        if (address.getText().toString().trim().length() < 10) {
            InputValidator.error(
                    address, "Nhập địa chỉ đầy đủ: số nhà, đường, phường/xã, tỉnh/thành");
            first = address;
        }
        if (!InputValidator.validPhone(phone.getText().toString())) {
            InputValidator.error(phone, "Nhập số điện thoại Việt Nam hợp lệ");
            first = phone;
        }
        if (name.getText().toString().trim().length() < 2) {
            InputValidator.error(name, "Nhập tên người nhận");
            first = name;
        }
        if (first != null) first.requestFocus();
        return first == null;
    }

    public static String normalizePhone(String value) {
        return value.replaceAll("[\\s().-]", "");
    }

    public static boolean validPhone(String value) {
        return normalizePhone(value).matches("(?:0|\\+84)[35789][0-9]{8}");
    }

    public static void error(EditText field, String message) {
        ViewParent parent = field.getParent();
        while (parent instanceof View) {
            if (parent instanceof TextInputLayout) {
                ((TextInputLayout) parent).setError(message);
                return;
            }
            parent = parent.getParent();
        }
        field.setError(message);
    }
}
