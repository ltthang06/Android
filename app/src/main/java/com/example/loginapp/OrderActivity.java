package com.example.loginapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class OrderActivity extends AppCompatActivity {
    public static final String EXTRA_ORDER_ID = "order_id";
    public static final String EXTRA_CART_IDS = "selected_cart_ids";
    public static final String EXTRA_REBUY = "rebuy_order_id";
    public static final String EXTRA_HISTORY = "order_history";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<Map<String, Object>> items = new ArrayList<>();
    private LinearLayout content;
    private CollectionReference cart;
    private CollectionReference orders;
    private boolean direct;
    private String completedOrderId;
    private MaterialButton confirm;
    private TextView summary;
    private LinearLayout productCards;
    private EditText recipientName, recipientPhone, deliveryAddress, orderNote;
    private String historyQuery = "";
    private long historyFrom = -1, historyTo = -1;
    private final List<DocumentSnapshot> historyOrders = new ArrayList<>();
    private LinearLayout historyCards;
    private TextView historyCount;
    private boolean historyLoaded;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String id = getSharedPreferences("session", MODE_PRIVATE).getString("userId", "");
        if (id.isEmpty()) {
            finish();
            return;
        }
        cart = db.collection("users").document(id).collection("cart");
        orders = db.collection("users").document(id).collection("orders");
        setContentView(R.layout.activity_order);
        ScreenInsets.apply(this);
        content = findViewById(R.id.orderContent);
        findViewById(R.id.btnOrderBack).setOnClickListener(v -> finish());
        if (state != null) {
            historyQuery = state.getString("historyQuery", "");
            historyFrom = state.getLong("historyFrom", -1);
            historyTo = state.getLong("historyTo", -1);
        }
        completedOrderId = state == null ? null : state.getString("completedOrderId");
        if (completedOrderId != null) {
            showOrder(completedOrderId);
            return;
        }
        if (getIntent().hasExtra(EXTRA_ORDER_ID)) {
            showOrder(getIntent().getStringExtra(EXTRA_ORDER_ID));
            return;
        }
        if (getIntent().getBooleanExtra(EXTRA_HISTORY, false)) {
            showHistory();
            return;
        }
        banner("Xác nhận đặt hàng", "Kiểm tra lựa chọn của bạn trước khi đặt.");
        productCards = new LinearLayout(this);
        productCards.setOrientation(LinearLayout.VERTICAL);
        content.addView(productCards, new LinearLayout.LayoutParams(-1, -2));
        View delivery = getLayoutInflater().inflate(R.layout.checkout_delivery, content, false);
        content.addView(delivery);
        View note = getLayoutInflater().inflate(R.layout.checkout_note, content, false);
        content.addView(note);
        orderNote = note.findViewById(R.id.orderNote);
        recipientName = delivery.findViewById(R.id.recipientName);
        recipientPhone = delivery.findViewById(R.id.recipientPhone);
        deliveryAddress = delivery.findViewById(R.id.deliveryAddress);
        MaterialButton chooseAddress =
                new MaterialButton(
                        this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        chooseAddress.setText(R.string.address_choose);
        chooseAddress.setCornerRadius(dp(14));
        chooseAddress.setMinHeight(dp(48));
        chooseAddress.setInsetTop(0);
        chooseAddress.setInsetBottom(0);
        LinearLayout.LayoutParams addressButtonParams = new LinearLayout.LayoutParams(-1, dp(48));
        addressButtonParams.topMargin = dp(16);
        addressButtonParams.bottomMargin = dp(4);
        ((LinearLayout) delivery).addView(chooseAddress, 1, addressButtonParams);
        chooseAddress.setOnClickListener(
                v -> {
                    Map<String, String> current = new HashMap<>();
                    current.put("name", recipientName.getText().toString().trim());
                    current.put(
                            "phone",
                            InputValidator.normalizePhone(recipientPhone.getText().toString()));
                    current.put("address", deliveryAddress.getText().toString().trim());
                    AddressBookActivity.choose(
                            this,
                            current,
                            address -> {
                                recipientName.setText(address.getString("name"));
                                recipientPhone.setText(address.getString("phone"));
                                deliveryAddress.setText(address.getString("address"));
                            },
                            () -> {
                                deliveryAddress.requestFocus();
                                deliveryAddress.post(
                                        () -> {
                                            InputMethodManager keyboard =
                                                    (InputMethodManager)
                                                            getSystemService(INPUT_METHOD_SERVICE);
                                            keyboard.showSoftInput(
                                                    deliveryAddress,
                                                    InputMethodManager.SHOW_IMPLICIT);
                                        });
                            });
                });
        if (state == null)
            db.collection("users")
                    .document(id)
                    .get()
                    .addOnSuccessListener(
                            user -> {
                                if (isFinishing() || isDestroyed()) return;
                                if (recipientName.length() == 0)
                                    recipientName.setText(user.getString("username"));
                                if (recipientPhone.length() == 0)
                                    recipientPhone.setText(user.getString("phone"));
                                String defaultId = user.getString("defaultAddressId");
                                if (defaultId != null)
                                    user.getReference()
                                            .collection("addresses")
                                            .document(defaultId)
                                            .get()
                                            .addOnSuccessListener(
                                                    address -> {
                                                        if (!isDestroyed()
                                                                && address.exists()
                                                                && deliveryAddress.length() == 0
                                                                && Objects.equals(
                                                                        recipientName
                                                                                .getText()
                                                                                .toString(),
                                                                        user.getString("username"))
                                                                && Objects.equals(
                                                                        recipientPhone
                                                                                .getText()
                                                                                .toString(),
                                                                        user.getString("phone"))) {
                                                            recipientName.setText(
                                                                    address.getString("name"));
                                                            recipientPhone.setText(
                                                                    address.getString("phone"));
                                                            deliveryAddress.setText(
                                                                    address.getString("address"));
                                                        }
                                                    });
                            });
        findViewById(R.id.orderFooter).setVisibility(View.VISIBLE);
        summary = findViewById(R.id.orderTotal);
        summary.setText(R.string.label_dang_tai_san_pham);
        confirm = findViewById(R.id.btnOrderConfirm);
        confirm.setEnabled(false);
        confirm.setOnClickListener(
                v -> {
                    if (!validateDelivery()) return;
                    AlertDialog dialog =
                            new MaterialAlertDialogBuilder(this)
                                    .setBackground(
                                            ContextCompat.getDrawable(this, R.drawable.bg_surface))
                                    .setTitle("Xác nhận đơn hàng?")
                                    .setMessage(
                                            getString(
                                                    R.string.checkout_confirm_message,
                                                    Money.format(total())))
                                    .setNegativeButton("Kiểm tra lại", null)
                                    .setPositiveButton("Đặt hàng", (d, w) -> placeOrder())
                                    .create();
                    dialog.setOnShowListener(
                            shown -> {
                                Button orderButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                orderButton.setTextColor(Color.WHITE);
                                orderButton.setBackgroundTintList(
                                        ColorStateList.valueOf(getColor(R.color.brand)));
                                orderButton.setPadding(dp(20), 0, dp(20), 0);
                                orderButton.setMinHeight(dp(48));
                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                        .setTextColor(getColor(R.color.brand));
                            });
                    dialog.show();
                });
        direct = getIntent().hasExtra(ProductDetailActivity.EXTRA_NAME);
        if (getIntent().hasExtra(EXTRA_REBUY)) {
            direct = true;
            loadRebuy(getIntent().getStringExtra(EXTRA_REBUY));
        } else if (direct) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", getIntent().getStringExtra(ProductDetailActivity.EXTRA_ID));
            item.put("name", getIntent().getStringExtra(ProductDetailActivity.EXTRA_NAME));
            item.put("price", getIntent().getIntExtra(ProductDetailActivity.EXTRA_PRICE_VALUE, 0));
            item.put("quantity", 1);
            item.put(
                    "image",
                    getResources()
                            .getResourceEntryName(
                                    getIntent()
                                            .getIntExtra(
                                                    ProductDetailActivity.EXTRA_IMAGE,
                                                    R.drawable.images)));
            if (PurchaseItems.valid(item)) items.add(item);
            displaySummary();
        } else
            cart.get()
                    .addOnSuccessListener(
                            result -> {
                                ArrayList<String> ids =
                                        getIntent().getStringArrayListExtra(EXTRA_CART_IDS);
                                Set<String> selected = ids == null ? null : new HashSet<>(ids);
                                for (DocumentSnapshot d : result) {
                                    if (!PurchaseItems.selected(selected, d.getId())
                                            || !PurchaseItems.valid(d.getData())) continue;
                                    Map<String, Object> item = new HashMap<>(d.getData());
                                    item.put("cartId", d.getId());
                                    items.add(item);
                                }
                                displaySummary();
                            })
                    .addOnFailureListener(
                            error -> {
                                summary.setText(R.string.label_khong_tai_duoc_gio_hang);
                                error(error);
                            });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putString("completedOrderId", completedOrderId);
        out.putString("historyQuery", historyQuery);
        out.putLong("historyFrom", historyFrom);
        out.putLong("historyTo", historyTo);
        super.onSaveInstanceState(out);
    }

    private void loadRebuy(String id) {
        orders.document(id)
                .get()
                .addOnSuccessListener(
                        order -> {
                            Object lines = order.get("items");
                            if (!(lines instanceof List<?>)) {
                                summary.setText(R.string.rebuy_empty);
                                return;
                            }
                            db.collection("products")
                                    .get()
                                    .addOnSuccessListener(
                                            products -> {
                                                if (isDestroyed() || isFinishing()) return;
                                                Map<String, DocumentSnapshot>
                                                        byId = new HashMap<>(),
                                                        byName = new HashMap<>();
                                                for (DocumentSnapshot product : products) {
                                                    byId.put(product.getId(), product);
                                                    byName.put(product.getString("name"), product);
                                                }
                                                boolean missing = false;
                                                for (Object line : (List<?>) lines) {
                                                    if (!(line instanceof Map<?, ?>)) continue;
                                                    Map<?, ?> old = (Map<?, ?>) line;
                                                    DocumentSnapshot match =
                                                            old.get("productId") == null
                                                                    ? byName.get(old.get("name"))
                                                                    : byId.get(
                                                                            old.get("productId"));
                                                    if (match == null
                                                            || match.getLong("price") == null) {
                                                        missing = true;
                                                        continue;
                                                    }
                                                    Map<String, Object> item = new HashMap<>();
                                                    item.put("productId", match.getId());
                                                    item.put("name", match.getString("name"));
                                                    item.put("price", match.getLong("price"));
                                                    item.put("image", match.getString("image"));
                                                    item.put("quantity", old.get("quantity"));
                                                    if (PurchaseItems.valid(item)) items.add(item);
                                                }
                                                displaySummary();
                                                if (missing)
                                                    text(
                                                            "Một số sản phẩm không còn bán nên đã"
                                                                + " được bỏ qua. Giá hiển thị là"
                                                                + " giá hiện tại.",
                                                            15);
                                            })
                                    .addOnFailureListener(
                                            e -> {
                                                summary.setText(R.string.rebuy_price_error);
                                                error(e);
                                            });
                        })
                .addOnFailureListener(
                        e -> {
                            summary.setText(R.string.rebuy_load_error);
                            error(e);
                        });
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(0xFF392357);
        view.setPadding(0, dp(12), 0, dp(12));
        content.addView(view);
        return view;
    }

    private MaterialButton button(String label) {
        MaterialButton view = new MaterialButton(this);
        view.setText(label);
        view.setCornerRadius(dp(16));
        view.setTextSize(15);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1, Math.round(56 * getResources().getDisplayMetrics().density));
        params.topMargin = Math.round(12 * getResources().getDisplayMetrics().density);
        content.addView(view, params);
        return view;
    }

    private long total() {
        long total = 0;
        for (Map<String, Object> item : items) total += PurchaseItems.lineTotal(item);
        return total;
    }

    private void displaySummary() {
        productCards.removeAllViews();
        for (Map<String, Object> item : items) productCard(productCards, item);
        summary.setText(items.isEmpty() ? "Giỏ hàng đang trống." : Money.format(total()));
        summary.setTextSize(22);
        summary.setTypeface(null, Typeface.BOLD);
        confirm.setText(items.isEmpty() ? "Đặt hàng" : "Đặt hàng · " + Money.format(total()));
        confirm.setEnabled(!items.isEmpty());
    }

    private boolean validateDelivery() {
        return InputValidator.checkDelivery(recipientName, recipientPhone, deliveryAddress);
    }

    private void placeOrder() {
        if (items.isEmpty() || !confirm.isEnabled() || !validateDelivery()) return;
        Map<String, Object> shipping = new HashMap<>();
        shipping.put("name", recipientName.getText().toString().trim());
        shipping.put("phone", InputValidator.normalizePhone(recipientPhone.getText().toString()));
        shipping.put("address", deliveryAddress.getText().toString().trim());
        String note = orderNote.getText().toString().trim();
        confirm.setEnabled(false);
        DocumentReference order = orders.document();
        db.runTransaction(
                        transaction -> {
                            if (!direct) {
                                for (Map<String, Object> item : items) {
                                    DocumentSnapshot current =
                                            transaction.get(
                                                    cart.document((String) item.get("cartId")));
                                    if (!current.exists()
                                            || !Objects.equals(
                                                    current.getLong("quantity"),
                                                    ((Number) item.get("quantity")).longValue())
                                            || !Objects.equals(
                                                    current.getLong("price"),
                                                    ((Number) item.get("price")).longValue()))
                                        throw new FirebaseFirestoreException(
                                                "Giỏ hàng đã thay đổi. Vui lòng quay lại và đặt"
                                                        + " lại.",
                                                FirebaseFirestoreException.Code.ABORTED);
                                }
                            }
                            Map<String, Object> data = new HashMap<>();
                            data.put("items", items);
                            data.put("total", total());
                            data.put("shipping", shipping);
                            if (!note.isEmpty()) data.put("note", note);
                            data.put("method", "COD");
                            data.put("paymentStatus", "Chưa thanh toán");
                            data.put("status", "Đặt mua thành công");
                            data.put("createdAt", FieldValue.serverTimestamp());
                            transaction.set(order, data);
                            if (!direct)
                                for (Map<String, Object> item : items)
                                    transaction.delete(cart.document((String) item.get("cartId")));
                            return null;
                        })
                .addOnSuccessListener(
                        done -> {
                            completedOrderId = order.getId();
                            findViewById(R.id.orderFooter).setVisibility(View.GONE);
                            content.removeAllViews();
                            banner(
                                    "✓  Đặt mua thành công",
                                    "Đơn hàng đã được lưu vào lịch sử của bạn.");
                            OrderCards.add(
                                    this,
                                    content,
                                    "CHỜ THANH TOÁN",
                                    "Cảm ơn bạn đã mua sắm!",
                                    "Mã đơn: " + order.getId(),
                                    "Bạn có thể xem lại sản phẩm và thông tin đơn bất cứ lúc nào.",
                                    Money.format(total()),
                                    "Thanh toán khi nhận hàng (COD)");
                            button("Xem chi tiết đơn hàng")
                                    .setOnClickListener(
                                            v -> {
                                                content.removeAllViews();
                                                showOrder(order.getId());
                                            });
                            MaterialButton browse = button("Tiếp tục mua sắm");
                            browse.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                            browse.setTextColor(getColor(R.color.brand));
                            browse.setStrokeColor(ColorStateList.valueOf(getColor(R.color.brand)));
                            browse.setStrokeWidth(dp(1));
                            browse.setOnClickListener(v -> finish());
                        })
                .addOnFailureListener(
                        e -> {
                            confirm.setEnabled(true);
                            error(e);
                        });
    }

    private void showOrder(String id) {
        banner("Chi tiết đơn hàng", "Thông tin đơn đặt mua của bạn");
        TextView loading = text("Đang tải đơn hàng…", 15);
        orders.document(id)
                .get()
                .addOnSuccessListener(
                        order -> {
                            content.removeView(loading);
                            if (!order.exists()) {
                                text("Không tìm thấy đơn hàng.", 16);
                                return;
                            }
                            Date date = order.getDate("createdAt");
                            String metadata = "Mã đơn: " + order.getId();
                            if (date != null)
                                metadata +=
                                        "\n"
                                                + new SimpleDateFormat(
                                                                "dd/MM/yyyy HH:mm",
                                                                Locale.getDefault())
                                                        .format(date);
                            View info =
                                    OrderCards.add(
                                            this,
                                            content,
                                            "ĐẶT MUA THÀNH CÔNG",
                                            "Thông tin đơn hàng",
                                            metadata,
                                            "COD".equals(order.getString("method"))
                                                    ? "Phương thức thanh toán: Thanh toán khi nhận"
                                                            + " hàng (COD)"
                                                    : "",
                                            "",
                                            "");
                            MaterialButton copy = new MaterialButton(this);
                            copy.setText(R.string.order_copy_id);
                            copy.setTextSize(14);
                            copy.setTextColor(getColor(R.color.brand));
                            copy.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                            copy.setCornerRadius(dp(12));
                            copy.setIconResource(R.drawable.ic_copy);
                            copy.setIconSize(dp(18));
                            ((LinearLayout) info)
                                    .addView(copy, new LinearLayout.LayoutParams(-2, dp(48)));
                            copy.setOnClickListener(
                                    v -> {
                                        ClipboardManager clipboard =
                                                (ClipboardManager)
                                                        getSystemService(CLIPBOARD_SERVICE);
                                        clipboard.setPrimaryClip(
                                                ClipData.newPlainText(
                                                        getString(R.string.order_id_label),
                                                        order.getId()));
                                        if (Build.VERSION.SDK_INT < 33)
                                            Toast.makeText(
                                                            this,
                                                            R.string.order_id_copied,
                                                            Toast.LENGTH_SHORT)
                                                    .show();
                                    });
                            Object shippingValue = order.get("shipping");
                            if (shippingValue instanceof Map<?, ?>) {
                                Map<?, ?> shipping = (Map<?, ?>) shippingValue;
                                String recipient = Objects.toString(shipping.get("name"), "");
                                View deliveryCard =
                                        OrderCards.add(
                                                this,
                                                content,
                                                "",
                                                "Thông tin giao hàng",
                                                "",
                                                recipient
                                                        + "\nSố điện thoại: "
                                                        + Objects.toString(
                                                                shipping.get("phone"), "")
                                                        + "\nĐịa chỉ: "
                                                        + Objects.toString(
                                                                shipping.get("address"), ""),
                                                "",
                                                "");
                                TextView details = deliveryCard.findViewById(R.id.orderCardItems);
                                SpannableString formatted = new SpannableString(details.getText());
                                if (!recipient.isEmpty())
                                    formatted.setSpan(
                                            new StyleSpan(Typeface.BOLD),
                                            0,
                                            recipient.length(),
                                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                details.setText(formatted);
                                details.setLineSpacing(dp(6), 1f);
                            }
                            String note = order.getString("note");
                            if (note != null && !note.trim().isEmpty())
                                OrderCards.add(
                                        this,
                                        content,
                                        "",
                                        getString(R.string.order_note_title),
                                        "",
                                        note,
                                        "",
                                        "");
                            Object lines = order.get("items");
                            if (lines instanceof List<?>)
                                for (Object entry : (List<?>) lines) {
                                    if (!(entry instanceof Map<?, ?>)) continue;
                                    productCard(content, (Map<?, ?>) entry);
                                }
                            Long total = order.getLong("total");
                            OrderCards.add(
                                    this,
                                    content,
                                    "",
                                    "Tổng giá trị đơn hàng",
                                    "",
                                    "",
                                    Money.format(total == null ? 0 : total),
                                    "");
                            button("Mua lại")
                                    .setOnClickListener(
                                            v ->
                                                    startActivity(
                                                            new Intent(this, OrderActivity.class)
                                                                    .putExtra(EXTRA_REBUY, id)));
                        })
                .addOnFailureListener(
                        error -> {
                            loading.setText(R.string.label_khong_tai_duoc_don_hang);
                            error(error);
                        });
    }

    private void showHistory() {
        banner("Đơn hàng của bạn", "Theo dõi các sản phẩm bạn đã đặt.");
        View filters = getLayoutInflater().inflate(R.layout.order_history_filters, content, false);
        content.addView(filters);
        EditText search = filters.findViewById(R.id.historySearch);
        Button from = filters.findViewById(R.id.historyFrom),
                to = filters.findViewById(R.id.historyTo);
        View clear = filters.findViewById(R.id.historyClear);
        historyCount = filters.findViewById(R.id.historyResultCount);
        historyCards = new LinearLayout(this);
        historyCards.setOrientation(LinearLayout.VERTICAL);
        content.addView(historyCards);
        Runnable refresh =
                () -> {
                    SimpleDateFormat format =
                            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    from.setText(
                            historyFrom < 0
                                    ? getString(R.string.history_from)
                                    : format.format(new Date(historyFrom)));
                    to.setText(
                            historyTo < 0
                                    ? getString(R.string.history_to)
                                    : format.format(new Date(historyTo)));
                    clear.setVisibility(
                            !historyQuery.trim().isEmpty() || historyFrom >= 0 || historyTo >= 0
                                    ? View.VISIBLE
                                    : View.GONE);
                    renderHistory();
                };
        search.setText(historyQuery);
        search.addTextChangedListener(
                new TextWatcher() {
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    public void onTextChanged(
                            CharSequence value, int start, int before, int count) {
                        historyQuery = value.toString();
                        refresh.run();
                    }

                    public void afterTextChanged(Editable value) {}
                });
        getSupportFragmentManager()
                .setFragmentResultListener(
                        VietnameseDatePicker.RESULT,
                        this,
                        (key, result) -> {
                            long chosen = result.getLong("day");
                            if (result.getBoolean("from")) historyFrom = chosen;
                            else historyTo = chosen;
                            refresh.run();
                        });
        from.setOnClickListener(v -> chooseHistoryDate(true));
        to.setOnClickListener(v -> chooseHistoryDate(false));
        clear.setOnClickListener(
                v -> {
                    historyFrom = -1;
                    historyTo = -1;
                    historyQuery = "";
                    search.setText("");
                    refresh.run();
                });
        historyCount.setText(R.string.shopping_loading);
        refresh.run();
        orders.orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(
                        result -> {
                            if (isDestroyed() || isFinishing()) return;
                            historyOrders.clear();
                            historyOrders.addAll(result.getDocuments());
                            historyLoaded = true;
                            refresh.run();
                        })
                .addOnFailureListener(
                        e -> {
                            historyCount.setText(R.string.label_khong_tai_duoc_don_hang);
                            error(e);
                        });
    }

    private void chooseHistoryDate(boolean from) {
        if (getSupportFragmentManager().findFragmentByTag(VietnameseDatePicker.TAG) != null) return;
        long minimum =
                !from && historyFrom >= 0 ? historyFrom : ShoppingFilters.dayStart(1900, 0, 1);
        long maximum = from && historyTo >= 0 ? historyTo : ShoppingFilters.dayStart(2100, 11, 31);
        long selected = from ? historyFrom : historyTo;
        long initial = selected < 0 ? System.currentTimeMillis() : selected;
        initial = Math.max(minimum, Math.min(initial, maximum));
        VietnameseDatePicker.create(from, initial, minimum, maximum)
                .show(getSupportFragmentManager(), VietnameseDatePicker.TAG);
    }

    private void renderHistory() {
        if (!historyLoaded) return;
        historyCards.removeAllViews();
        int count = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        for (DocumentSnapshot order : historyOrders) {
            Date date = order.getDate("createdAt");
            if (!ShoppingFilters.orderMatches(
                    order.getId(), historyQuery, date, historyFrom, historyTo)) continue;
            count++;
            String code = order.getId();
            String metadata = "Mã đơn: " + (code.length() > 8 ? code.substring(0, 8) + "…" : code);
            if (date != null) metadata += "\n" + dateFormat.format(date);
            StringBuilder products = new StringBuilder();
            Object lines = order.get("items");
            int productCount = 0;
            long quantity = 0;
            if (lines instanceof List<?>)
                for (Object entry : (List<?>) lines)
                    if (entry instanceof Map<?, ?>) {
                        Map<?, ?> item = (Map<?, ?>) entry;
                        if (!PurchaseItems.valid(item)) continue;
                        productCount++;
                        quantity += ((Number) item.get("quantity")).longValue();
                        if (products.length() > 0) products.append("\n");
                        products.append(item.get("name"))
                                .append(" × ")
                                .append(item.get("quantity"));
                    }
            View shell =
                    OrderCards.add(
                            this,
                            historyCards,
                            "ĐẶT MUA THÀNH CÔNG",
                            getResources()
                                    .getQuantityString(
                                            R.plurals.order_item_summary,
                                            productCount,
                                            productCount,
                                            quantity),
                            metadata,
                            products.toString(),
                            Money.format(
                                    order.getLong("total") == null ? 0 : order.getLong("total")),
                            "Xem chi tiết đơn hàng  ›");
            shell.findViewById(R.id.orderCardTotalLabel).setVisibility(View.VISIBLE);
            OrderCards.thumbnails(this, shell, lines);
            shell.setOnClickListener(
                    v -> {
                        Intent detail = new Intent(this, OrderActivity.class);
                        detail.putExtra(EXTRA_ORDER_ID, order.getId());
                        startActivity(detail);
                    });
        }
        historyCount.setText(
                count == 0
                        ? getString(R.string.history_no_match)
                        : getString(R.string.history_count, count));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void banner(String title, String subtitle) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(24), dp(24), dp(24), dp(24));
        FrameLayout banner = new FrameLayout(this);
        banner.setBackgroundResource(R.drawable.bg_about_banner);
        banner.setClipToOutline(true);
        ImageView orbits = new ImageView(this);
        orbits.setImageResource(R.drawable.login_orbits);
        orbits.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams decoration =
                new FrameLayout.LayoutParams(dp(180), dp(180), Gravity.TOP | Gravity.END);
        decoration.topMargin = -dp(46);
        decoration.setMarginEnd(-dp(42));
        banner.addView(orbits, decoration);
        banner.addView(header, new FrameLayout.LayoutParams(-1, -2));
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextSize(26);
        heading.setTextColor(0xFFFFFFFF);
        heading.setTypeface(null, Typeface.BOLD);
        header.addView(heading);
        TextView description = new TextView(this);
        description.setText(subtitle);
        description.setTextColor(0xFFE7DDF7);
        description.setTextSize(15);
        description.setPadding(0, dp(10), 0, 0);
        header.addView(description);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(20);
        content.addView(banner, params);
    }

    private void productCard(LinearLayout parent, Map<?, ?> item) {
        if (!PurchaseItems.valid(item)) return;
        View row = getLayoutInflater().inflate(R.layout.item_order_product, parent, false);
        ImageView image = row.findViewById(R.id.orderItemImage);
        Object imageName = item.get("image");
        int resource = ProductImages.find(imageName instanceof String ? (String) imageName : null);
        if (resource != 0) image.setImageResource(resource);
        image.setContentDescription(String.valueOf(item.get("name")));
        TextView details = row.findViewById(R.id.orderItemText);
        long price = ((Number) item.get("price")).longValue();
        long quantity = ((Number) item.get("quantity")).longValue();
        details.setText(
                getString(
                        R.string.order_product_details,
                        item.get("name"),
                        quantity,
                        Money.format(price)));
        ((TextView) row.findViewById(R.id.orderItemTotal))
                .setText(getString(R.string.order_line_total, Money.format(price * quantity)));
        parent.addView(row);
    }

    private void error(Exception e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    }
}
