package com.example.loginapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.function.Consumer;

public class AddressBookActivity extends AppCompatActivity {
    private LinearLayout content;
    private DocumentReference profile;
    private int request;
    static final String EXTRA_CREATE = "create_address";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (getSharedPreferences("session", MODE_PRIVATE).getString("userId", "").isEmpty()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_order);
        ScreenInsets.apply(this);
        content = findViewById(R.id.orderContent);
        findViewById(R.id.btnOrderBack).setOnClickListener(v -> finish());
        profile = AccountAuth.userDocument(this);
        load();
        if (state == null && getIntent().getBooleanExtra(EXTRA_CREATE, false)) edit(null);
    }

    private MaterialButton button(String label, LinearLayout parent, Runnable action) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setMinHeight(dp(48));
        parent.addView(button, new LinearLayout.LayoutParams(-1, -2));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private void load() {
        int token = ++request;
        content.removeAllViews();
        TextView heading = new TextView(this);
        heading.setText(R.string.address_book_title);
        heading.setTextSize(26);
        heading.setTextColor(getColor(R.color.text_primary));
        heading.setTypeface(null, Typeface.BOLD);
        content.addView(heading);
        MaterialButton addAddress =
                button(getString(R.string.address_add), content, () -> edit(null));
        addAddress.setTextSize(16);
        addAddress.setTypeface(addAddress.getTypeface(), Typeface.BOLD);
        addAddress.setInsetTop(0);
        addAddress.setInsetBottom(0);
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(-1, dp(48));
        addParams.topMargin = dp(16);
        addAddress.setLayoutParams(addParams);
        TextView status = new TextView(this);
        status.setText(R.string.address_loading);
        content.addView(status);
        profile.get()
                .addOnSuccessListener(
                        user ->
                                profile.collection("addresses")
                                        .get()
                                        .addOnSuccessListener(
                                                result -> {
                                                    if (isDestroyed() || token != request) return;
                                                    status.setText(
                                                            result.isEmpty()
                                                                    ? "Lưu địa chỉ để đặt hàng"
                                                                            + " nhanh hơn."
                                                                    : "");
                                                    for (DocumentSnapshot address : result) {
                                                        boolean isDefault =
                                                                address.getId()
                                                                        .equals(
                                                                                user.getString(
                                                                                        "defaultAddressId"));
                                                        LinearLayout card =
                                                                (LinearLayout)
                                                                        OrderCards.add(
                                                                                this,
                                                                                content,
                                                                                isDefault
                                                                                        ? "MẶC ĐỊNH"
                                                                                        : "",
                                                                                address.getString(
                                                                                        "name"),
                                                                                "",
                                                                                "Số điện thoại: "
                                                                                        + address
                                                                                                .getString(
                                                                                                        "phone")
                                                                                        + "\n"
                                                                                        + "Địa chỉ:"
                                                                                        + " "
                                                                                        + address
                                                                                                .getString(
                                                                                                        "address"),
                                                                                "",
                                                                                "");
                                                        if (!isDefault) {
                                                            MaterialButton makeDefault =
                                                                    button(
                                                                            "Đặt làm mặc định",
                                                                            card,
                                                                            () ->
                                                                                    profile.update(
                                                                                                    "defaultAddressId",
                                                                                                    address
                                                                                                            .getId())
                                                                                            .addOnSuccessListener(
                                                                                                    done ->
                                                                                                            load())
                                                                                            .addOnFailureListener(
                                                                                                    this
                                                                                                            ::error));
                                                            makeDefault.setBackgroundTintList(
                                                                    ColorStateList.valueOf(
                                                                            Color.TRANSPARENT));
                                                            makeDefault.setTextColor(
                                                                    getColor(R.color.brand));
                                                        }
                                                        LinearLayout actions =
                                                                new LinearLayout(this);
                                                        actions.setGravity(Gravity.CENTER_VERTICAL);
                                                        LinearLayout.LayoutParams actionsParams =
                                                                new LinearLayout.LayoutParams(
                                                                        -1, -2);
                                                        actionsParams.topMargin = dp(16);
                                                        card.addView(actions, actionsParams);
                                                        MaterialButton edit =
                                                                addressAction(
                                                                        R.string.address_edit_title,
                                                                        R.drawable.ic_address_edit,
                                                                        0xFFF0E9FC,
                                                                        getColor(R.color.brand));
                                                        LinearLayout.LayoutParams editParams =
                                                                new LinearLayout.LayoutParams(
                                                                        0, dp(48), 1);
                                                        editParams.setMarginEnd(dp(12));
                                                        actions.addView(edit, editParams);
                                                        edit.setOnClickListener(v -> edit(address));
                                                        MaterialButton delete =
                                                                addressAction(
                                                                        R.string
                                                                                .address_delete_action,
                                                                        R.drawable
                                                                                .ic_address_delete,
                                                                        0xFFFFEBEE,
                                                                        0xFFB3261E);
                                                        actions.addView(
                                                                delete,
                                                                new LinearLayout.LayoutParams(
                                                                        0, dp(48), 1));
                                                        delete.setOnClickListener(
                                                                v -> confirmDelete(address));
                                                    }
                                                })
                                        .addOnFailureListener(
                                                e -> {
                                                    status.setText(R.string.address_load_error);
                                                    error(e);
                                                }))
                .addOnFailureListener(
                        e -> {
                            status.setText(R.string.profile_load_error);
                            error(e);
                        });
    }

    private MaterialButton addressAction(int label, int icon, int background, int foreground) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setIconResource(icon);
        button.setCornerRadius(dp(14));
        button.setStrokeWidth(0);
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setTextColor(foreground);
        button.setIconTint(ColorStateList.valueOf(foreground));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setTextSize(14);
        button.setMaxLines(1);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(6), 0, dp(6), 0);
        button.setIconSize(dp(18));
        button.setIconPadding(dp(4));
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setTypeface(button.getTypeface(), Typeface.BOLD);
        return button;
    }

    private void confirmDelete(DocumentSnapshot address) {
        TextView title = new TextView(this);
        title.setText(R.string.address_delete_title);
        title.setTextSize(22);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(24), dp(24), dp(24), dp(12));
        AlertDialog confirmation =
                new MaterialAlertDialogBuilder(this)
                        .setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_surface))
                        .setCustomTitle(title)
                        .setMessage(R.string.address_delete_hint)
                        .setNegativeButton(R.string.address_keep, null)
                        .setPositiveButton(
                                R.string.address_delete_action, (dialog, which) -> delete(address))
                        .create();
        confirmation.setOnShowListener(
                shown -> {
                    Button keep = confirmation.getButton(AlertDialog.BUTTON_NEGATIVE);
                    keep.setTextColor(getColor(R.color.brand));
                    Button remove = confirmation.getButton(AlertDialog.BUTTON_POSITIVE);
                    remove.setTextColor(0xFFB3261E);
                    remove.setBackgroundTintList(ColorStateList.valueOf(0xFFFFEBEE));
                    remove.setMinHeight(dp(48));
                    remove.setPadding(dp(16), 0, dp(16), 0);
                });
        confirmation.show();
    }

    private void delete(DocumentSnapshot address) {
        FirebaseFirestore.getInstance()
                .runTransaction(
                        tx -> {
                            DocumentSnapshot user = tx.get(profile);
                            tx.delete(address.getReference());
                            if (address.getId().equals(user.getString("defaultAddressId")))
                                tx.update(profile, "defaultAddressId", FieldValue.delete());
                            return null;
                        })
                .addOnSuccessListener(done -> load())
                .addOnFailureListener(this::error);
    }

    private void edit(DocumentSnapshot saved) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout form =
                (LinearLayout)
                        getLayoutInflater().inflate(R.layout.checkout_delivery, scroll, false);
        form.setBackground(null);
        ((TextView) form.getChildAt(0))
                .setText(
                        saved == null
                                ? R.string.address_create_title
                                : R.string.address_edit_title);
        EditText name = form.findViewById(R.id.recipientName),
                phone = form.findViewById(R.id.recipientPhone),
                address = form.findViewById(R.id.deliveryAddress);
        if (saved != null) {
            name.setText(saved.getString("name"));
            phone.setText(saved.getString("phone"));
            address.setText(saved.getString("address"));
        }
        MaterialCheckBox makeDefault = new MaterialCheckBox(this);
        makeDefault.setButtonDrawable(
                AppCompatResources.getDrawable(this, R.drawable.cart_checkbox));
        makeDefault.setButtonIconDrawable(null);
        makeDefault.setButtonTintList(null);
        makeDefault.setUseMaterialThemeColors(false);
        makeDefault.setText(R.string.address_default_option);
        LinearLayout.LayoutParams defaultParams = new LinearLayout.LayoutParams(-1, -2);
        defaultParams.topMargin = dp(12);
        makeDefault.setMinHeight(dp(48));
        form.addView(makeDefault, defaultParams);
        if (saved != null)
            profile.get()
                    .addOnSuccessListener(
                            user -> {
                                if (saved.getId().equals(user.getString("defaultAddressId"))) {
                                    makeDefault.setChecked(true);
                                    makeDefault.setEnabled(false);
                                }
                            });
        scroll.addView(form, new ScrollView.LayoutParams(-1, -2));
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_surface))
                        .setView(scroll)
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Lưu địa chỉ", null)
                        .create();
        dialog.setOnShowListener(
                shown -> {
                    Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    save.setTextColor(Color.WHITE);
                    save.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.brand)));
                    save.setMinHeight(dp(48));
                    save.setPadding(dp(16), 0, dp(16), 0);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(getColor(R.color.brand));
                    save.setOnClickListener(
                            v -> {
                                if (!InputValidator.checkDelivery(name, phone, address)) return;
                                Map<String, Object> data = new HashMap<>();
                                data.put("name", name.getText().toString().trim());
                                data.put(
                                        "phone",
                                        InputValidator.normalizePhone(phone.getText().toString()));
                                data.put("address", address.getText().toString().trim());
                                DocumentReference target =
                                        saved == null
                                                ? profile.collection("addresses").document()
                                                : saved.getReference();
                                boolean useAsDefault = makeDefault.isChecked();
                                dialog.getButton(-1).setEnabled(false);
                                dialog.getButton(-2).setEnabled(false);
                                dialog.setCancelable(false);
                                FirebaseFirestore.getInstance()
                                        .runTransaction(
                                                tx -> {
                                                    DocumentSnapshot user = tx.get(profile);
                                                    tx.set(target, data);
                                                    if (useAsDefault
                                                            || user.getString("defaultAddressId")
                                                                    == null)
                                                        tx.update(
                                                                profile,
                                                                "defaultAddressId",
                                                                target.getId());
                                                    return null;
                                                })
                                        .addOnSuccessListener(
                                                done -> {
                                                    dialog.dismiss();
                                                    load();
                                                })
                                        .addOnFailureListener(
                                                e -> {
                                                    dialog.getButton(-1).setEnabled(true);
                                                    dialog.getButton(-2).setEnabled(true);
                                                    dialog.setCancelable(true);
                                                    error(e);
                                                });
                            });
                });
        dialog.show();
    }

    static void choose(
            AppCompatActivity screen,
            Map<String, String> current,
            Consumer<DocumentSnapshot> chosen,
            Runnable enterManually) {
        AccountAuth.userDocument(screen)
                .collection("addresses")
                .get()
                .addOnSuccessListener(
                        result -> {
                            if (screen.isDestroyed() || screen.isFinishing()) return;
                            if (result.isEmpty()) {
                                TextView title = new TextView(screen);
                                title.setText(R.string.address_empty_title);
                                title.setTextSize(22);
                                title.setTypeface(null, Typeface.BOLD);
                                title.setTextColor(screen.getColor(R.color.text_primary));
                                int padding =
                                        Math.round(
                                                24
                                                        * screen.getResources()
                                                                .getDisplayMetrics()
                                                                .density);
                                title.setPadding(padding, padding, padding, padding / 2);
                                AlertDialog dialog =
                                        new MaterialAlertDialogBuilder(screen)
                                                .setBackground(
                                                        AppCompatResources.getDrawable(
                                                                screen, R.drawable.bg_surface))
                                                .setCustomTitle(title)
                                                .setMessage(R.string.address_empty_hint)
                                                .setNegativeButton(
                                                        R.string.address_manual,
                                                        (d, w) -> enterManually.run())
                                                .setPositiveButton(
                                                        R.string.address_add,
                                                        (d, w) ->
                                                                screen.startActivity(
                                                                        new Intent(
                                                                                        screen,
                                                                                        AddressBookActivity
                                                                                                .class)
                                                                                .putExtra(
                                                                                        EXTRA_CREATE,
                                                                                        true)))
                                                .create();
                                dialog.setOnShowListener(
                                        shown -> {
                                            Button add =
                                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                            add.setTextColor(Color.WHITE);
                                            add.setBackgroundTintList(
                                                    ColorStateList.valueOf(
                                                            screen.getColor(R.color.brand)));
                                            add.setMinHeight(padding * 2);
                                            add.setPadding(padding / 2, 0, padding / 2, 0);
                                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                                    .setTextColor(screen.getColor(R.color.brand));
                                        });
                                dialog.show();
                                return;
                            }
                            List<DocumentSnapshot> addresses = result.getDocuments();
                            AccountAuth.userDocument(screen)
                                    .get()
                                    .addOnSuccessListener(
                                            user ->
                                                    showChooser(
                                                            screen,
                                                            addresses,
                                                            user.getString("defaultAddressId"),
                                                            current,
                                                            chosen))
                                    .addOnFailureListener(
                                            e ->
                                                    showChooser(
                                                            screen, addresses, null, current,
                                                            chosen));
                        })
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                                screen,
                                                "Không tải được sổ địa chỉ. Vui lòng thử lại.",
                                                Toast.LENGTH_LONG)
                                        .show());
    }

    private static void showChooser(
            AppCompatActivity screen,
            List<DocumentSnapshot> addresses,
            String defaultId,
            Map<String, String> current,
            Consumer<DocumentSnapshot> chosen) {
        if (screen.isDestroyed() || screen.isFinishing()) return;
        int padding = Math.round(24 * screen.getResources().getDisplayMetrics().density);
        ScrollView scroll = new ScrollView(screen);
        LinearLayout choices = new LinearLayout(screen);
        choices.setOrientation(LinearLayout.VERTICAL);
        choices.setPadding(padding, padding / 2, padding, 0);
        scroll.addView(choices);
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(screen)
                        .setBackground(
                                AppCompatResources.getDrawable(screen, R.drawable.bg_surface))
                        .setTitle(R.string.address_choose_title)
                        .setView(scroll)
                        .setNeutralButton(
                                R.string.address_manage,
                                (d, w) ->
                                        screen.startActivity(
                                                new Intent(screen, AddressBookActivity.class)))
                        .setNegativeButton("Đóng", null)
                        .create();
        for (DocumentSnapshot address : addresses) {
            View row =
                    screen.getLayoutInflater()
                            .inflate(R.layout.item_address_choice, choices, false);
            boolean active =
                    Objects.equals(current.get("name"), address.getString("name"))
                            && Objects.equals(current.get("phone"), address.getString("phone"))
                            && Objects.equals(current.get("address"), address.getString("address"));
            row.setActivated(active);
            ((RadioButton) row.findViewById(R.id.addressChoiceRadio)).setChecked(active);
            ((TextView) row.findViewById(R.id.addressChoiceName))
                    .setText(address.getString("name"));
            ((TextView) row.findViewById(R.id.addressChoicePhone))
                    .setText(address.getString("phone"));
            ((TextView) row.findViewById(R.id.addressChoiceStreet))
                    .setText(address.getString("address"));
            row.findViewById(R.id.addressChoiceDefault)
                    .setVisibility(address.getId().equals(defaultId) ? View.VISIBLE : View.GONE);
            row.setOnClickListener(
                    v -> {
                        chosen.accept(address);
                        dialog.dismiss();
                    });
            choices.addView(row);
        }
        dialog.show();
    }

    private void error(Exception e) {
        if (!isDestroyed()) Toast.makeText(this, AccountAuth.message(e), Toast.LENGTH_LONG).show();
    }
}
