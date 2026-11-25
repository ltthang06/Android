package com.example.loginapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntConsumer;

public class ProductActivity extends AppCompatActivity {
    private TextView tvWelcome;
    private String username;
    private int cartRequest, extraRequest;
    private boolean selectingAll;
    private ProductFilterState filters = new ProductFilterState();
    private final Map<String, Double> averageRatings = new HashMap<>();
    private boolean ratingsLoaded;
    private int ratingRequest;
    private List<Product> catalog;
    private Runnable refreshCatalog;
    private final Set<String> excludedCartItems = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser signedIn = AccountAuth.auth().getCurrentUser();
        if (getSharedPreferences("session", MODE_PRIVATE).getString("userId", "").isEmpty()
                || signedIn == null
                || !signedIn.isEmailVerified()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        if (savedInstanceState != null) {
            filters.category = savedInstanceState.getInt("filterCategory", 0);
            filters.sort = savedInstanceState.getInt("filterSort", 0);
            filters.price = savedInstanceState.getInt("filterPrice", 0);
            filters.rating = savedInstanceState.getInt("filterRating", 0);
            filters.brand = savedInstanceState.getString("filterBrand", "");
            ArrayList<String> excluded = savedInstanceState.getStringArrayList("excludedCartItems");
            if (excluded != null) excludedCartItems.addAll(excluded);
        }
        setContentView(R.layout.activity_product);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        View drawerPanel = findViewById(R.id.leftMenu);
        int drawerPadding = Math.round(16 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(
                drawerPanel,
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(
                            drawerPadding + bars.left,
                            drawerPadding + bars.top,
                            drawerPadding + bars.right,
                            drawerPadding + bars.bottom);
                    return insets;
                });
        ViewCompat.requestApplyInsets(drawerPanel);
        findViewById(R.id.drawerProfile).setClipToOutline(true);
        Button btnMenu = findViewById(R.id.btnMenu);
        ImageButton btnProfile = findViewById(R.id.btnProfile);
        Button btnMenuAccount = findViewById(R.id.btnMenuAccount);
        Button btnMenuChangePassword = findViewById(R.id.btnMenuChangePassword);
        Button btnMenuLogout = findViewById(R.id.btnMenuLogout);

        tvWelcome = findViewById(R.id.tvWelcome);
        RecyclerView listView = findViewById(R.id.listView);
        listView.setLayoutManager(new GridLayoutManager(this, 2));
        View productHeader =
                getLayoutInflater().inflate(R.layout.view_product_header, listView, false);
        TextView tvTitle = findViewById(R.id.tvTitle);
        View cartTabContent = findViewById(R.id.cartTabContent);
        LinearLayout cartTabItems = findViewById(R.id.cartTabItems);
        refreshUsername();

        List<Product> products = new ArrayList<>();
        catalog = products;
        List<Product> visibleProducts = new ArrayList<>();
        ProductGridAdapter adapter =
                new ProductGridAdapter(
                        listView,
                        productHeader,
                        visibleProducts,
                        product ->
                                startActivity(ProductDetailActivity.createIntent(this, product)));
        EditText search = productHeader.findViewById(R.id.edtSearch);
        TextView searchEmpty = productHeader.findViewById(R.id.tvSearchEmpty);
        ChipGroup categoryChips = productHeader.findViewById(R.id.categoryChips);
        MaterialButton filterButton = productHeader.findViewById(R.id.openProductFilters);
        filterButton.setEnabled(false);
        TextView resultCount = productHeader.findViewById(R.id.productResultCount);
        TextView filterSummary = productHeader.findViewById(R.id.productFilterSummary);
        String[] categories = getResources().getStringArray(R.array.filter_categories);
        Runnable filterProducts =
                () -> {
                    String query = search.getText().toString();
                    visibleProducts.clear();
                    for (Product product : products)
                        if (filters.matches(product, query, averageRatings))
                            visibleProducts.add(product);
                    if (filters.sort == 1)
                        visibleProducts.sort(Comparator.comparingInt(Product::getPrice));
                    if (filters.sort == 2)
                        visibleProducts.sort(Comparator.comparingInt(Product::getPrice).reversed());
                    adapter.submitProducts(visibleProducts);
                    filterButton.setEnabled(!products.isEmpty());
                    filterButton.setText(
                            filters.activeCount() == 0
                                    ? getString(R.string.product_filters_title)
                                    : getString(
                                            R.string.product_filters_active,
                                            filters.activeCount()));
                    for (int i = 0; i < categoryChips.getChildCount(); i++)
                        ((Chip) categoryChips.getChildAt(i)).setChecked(i == filters.category);
                    List<String> summary = new ArrayList<>();
                    if (filters.price > 0)
                        summary.add(
                                getResources()
                                        .getStringArray(R.array.price_preset_labels)[
                                        filters.price]);
                    if (!filters.brand.isEmpty()) summary.add(filters.brand);
                    if (filters.rating > 0)
                        summary.add(getString(R.string.filter_rating_summary, filters.rating));
                    if (filters.sort > 0)
                        summary.add(
                                getResources()
                                        .getStringArray(R.array.filter_sort_options)[filters.sort]);
                    filterSummary.setText(TextUtils.join(" · ", summary));
                    filterSummary.setVisibility(summary.isEmpty() ? View.GONE : View.VISIBLE);
                    resultCount.setText(
                            getResources()
                                    .getQuantityString(
                                            R.plurals.product_result_count,
                                            visibleProducts.size(),
                                            visibleProducts.size()));
                    searchEmpty.setVisibility(visibleProducts.isEmpty() ? View.VISIBLE : View.GONE);
                };
        refreshCatalog = filterProducts;
        filterButton.setOnClickListener(
                v -> showProductFilters(products, search.getText().toString(), filterProducts));
        for (int index = 0; index < categories.length; index++) {
            final int categoryIndex = index;
            Chip chip = filterChip(categories[index]);
            categoryChips.addView(chip);
            chip.setChecked(index == filters.category);
            chip.setOnClickListener(
                    v -> {
                        filters.category = categoryIndex;
                        filterProducts.run();
                    });
        }
        search.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence text, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence text, int start, int before, int count) {
                        filterProducts.run();
                    }

                    @Override
                    public void afterTextChanged(Editable text) {}
                });
        listView.setAdapter(adapter);
        loadProducts(products, filterProducts, searchEmpty);

        BottomNavigationView navigation = findViewById(R.id.tabBar);
        View productRoot = findViewById(R.id.productRoot);
        ViewCompat.setOnApplyWindowInsetsListener(
                navigation,
                (view, insets) -> {
                    int bottom =
                            Math.max(
                                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom,
                                    insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
                    view.setPadding(0, 0, 0, 0);
                    productRoot.setPadding(dp(16), dp(16), dp(16), dp(16) + bottom);
                    return insets;
                });
        ViewCompat.requestApplyInsets(navigation);
        View extra = findViewById(R.id.extraTabContent);
        LinearLayout extraItems = findViewById(R.id.extraTabItems);
        navigation.setOnItemSelectedListener(
                item -> {
                    int selected = item.getItemId();
                    extraRequest++;
                    searchEmpty.setVisibility(View.GONE);
                    if (selected == R.id.nav_products) filterProducts.run();
                    listView.setVisibility(
                            selected == R.id.nav_products ? View.VISIBLE : View.GONE);
                    cartTabContent.setVisibility(
                            selected == R.id.nav_cart ? View.VISIBLE : View.GONE);
                    extra.setVisibility(
                            selected == R.id.nav_account || selected == R.id.nav_notifications
                                    ? View.VISIBLE
                                    : View.GONE);
                    tvTitle.setVisibility(selected == R.id.nav_products ? View.GONE : View.VISIBLE);
                    if (selected == R.id.nav_products)
                        tvTitle.setText(R.string.label_kham_pha_cong_nghe);
                    if (selected == R.id.nav_cart) {
                        tvTitle.setText(R.string.label_gio_hang_2);
                        showCartItems(cartTabItems);
                    }
                    if (selected == R.id.nav_account) {
                        tvTitle.setText(R.string.label_tai_khoan_cua_ban);
                        showAccount(extraItems);
                    }
                    if (selected == R.id.nav_notifications) {
                        tvTitle.setText(R.string.label_thong_bao_2);
                        showNotifications(extraItems);
                    }
                    return true;
                });

        findViewById(R.id.drawerProfile).setOnClickListener(v -> btnMenuAccount.performClick());
        btnMenu.setOnClickListener(
                v -> {
                    btnMenuAccount.setSelected(navigation.getSelectedItemId() == R.id.nav_account);
                    ((TextView) findViewById(R.id.drawerName)).setText(username);
                    ((TextView) findViewById(R.id.drawerAvatar))
                            .setText(
                                    username.isEmpty()
                                            ? "?"
                                            : username.substring(0, 1).toUpperCase(Locale.ROOT));
                    ((TextView) findViewById(R.id.drawerEmail)).setText(AccountAuth.currentEmail());
                    drawerLayout.openDrawer(findViewById(R.id.leftMenu));
                });

        btnMenuAccount.setOnClickListener(
                v -> {
                    drawerLayout.closeDrawer(findViewById(R.id.leftMenu));
                    openProfile();
                });

        btnProfile.setOnClickListener(v -> openProfile());

        btnMenuChangePassword.setOnClickListener(
                v -> {
                    drawerLayout.closeDrawer(findViewById(R.id.leftMenu));
                    showChangePasswordDialog();
                });

        findViewById(R.id.btnOrders)
                .setOnClickListener(
                        v -> {
                            drawerLayout.closeDrawers();
                            openHistory();
                        });
        findViewById(R.id.btnMenuAbout)
                .setOnClickListener(
                        v -> {
                            drawerLayout.closeDrawers();
                            startActivity(new Intent(this, AboutActivity.class));
                        });
        btnMenuLogout.setOnClickListener(v -> confirmLogout());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("open_products", false)) {
            BottomNavigationView nav = findViewById(R.id.tabBar);
            if (nav != null) nav.setSelectedItemId(R.id.nav_products);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putInt("filterCategory", filters.category);
        out.putInt("filterSort", filters.sort);
        out.putInt("filterPrice", filters.price);
        out.putInt("filterRating", filters.rating);
        out.putString("filterBrand", filters.brand);
        out.putStringArrayList("excludedCartItems", new ArrayList<>(excludedCartItems));
        super.onSaveInstanceState(out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvWelcome == null) return;
        refreshUsername();
        ratingsLoaded = false;
        if (filters.rating > 0 && catalog != null && !catalog.isEmpty())
            loadRatings(
                    catalog,
                    refreshCatalog,
                    () ->
                            Toast.makeText(this, R.string.filter_ratings_failed, Toast.LENGTH_LONG)
                                    .show());
        BottomNavigationView nav = findViewById(R.id.tabBar);
        if (nav != null && nav.getSelectedItemId() == R.id.nav_account)
            showAccount(findViewById(R.id.extraTabItems));
        View cartView = findViewById(R.id.cartTabContent);
        if (cartView != null && cartView.getVisibility() == View.VISIBLE)
            showCartItems(findViewById(R.id.cartTabItems));
        else refreshCartBadge();
    }

    private void refreshUsername() {
        username =
                getSharedPreferences("session", Context.MODE_PRIVATE).getString("currentUser", "");
        if (tvWelcome != null) {
            tvWelcome.setText(getString(R.string.welcome_user, username));
        }
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        getSharedPreferences("session", Context.MODE_PRIVATE)
                .edit()
                .remove("currentUser")
                .remove("userId")
                .remove("secureAuth")
                .apply();

        Intent intent = new Intent(ProductActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void loadProducts(List<Product> products, Runnable filter, TextView status) {
        status.setText(R.string.label_dang_tai_san_pham);
        status.setVisibility(View.VISIBLE);
        status.setOnClickListener(null);
        FirebaseFirestore.getInstance()
                .collection("products")
                .get()
                .addOnSuccessListener(
                        result -> {
                            products.clear();
                            for (DocumentSnapshot doc : result) {
                                String name = doc.getString("name");
                                Long price = doc.getLong("price");
                                if (name != null && price != null)
                                    products.add(
                                            new Product(
                                                    doc.getId(),
                                                    name,
                                                    price.intValue(),
                                                    ProductImages.find(doc.getString("image")),
                                                    doc.getString("description")));
                            }
                            status.setText(R.string.label_khong_tim_thay_san_pham_phu_hop);
                            filter.run();
                            if (filters.rating > 0)
                                loadRatings(
                                        products,
                                        filter,
                                        () ->
                                                Toast.makeText(
                                                                this,
                                                                R.string.filter_ratings_failed,
                                                                Toast.LENGTH_LONG)
                                                        .show());
                        })
                .addOnFailureListener(
                        error -> {
                            status.setText(R.string.products_retry);
                            status.setVisibility(View.VISIBLE);
                            status.setOnClickListener(v -> loadProducts(products, filter, status));
                        });
    }

    private void showProductFilters(List<Product> products, String query, Runnable apply) {
        ProductFilterState draft = filters.copy();
        TreeSet<String> brandSet = new TreeSet<>();
        for (Product product : products) {
            String brand = ProductFilterState.brand(product.getName());
            if (!brand.isEmpty()) brandSet.add(brand);
        }
        List<String> brands = new ArrayList<>();
        brands.add("");
        brands.addAll(brandSet);
        String[] brandLabels = brands.toArray(new String[0]);
        brandLabels[0] = getString(R.string.filter_all_brands);
        if (!brands.contains(draft.brand)) draft.brand = "";
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(20));
        root.setBackgroundResource(R.drawable.bg_surface);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(top);
        TextView title = new TextView(this);
        title.setText(R.string.product_filters_title);
        title.setTextSize(24);
        title.setTextColor(getColor(R.color.text_primary));
        title.setTypeface(null, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        MaterialButton reset =
                new MaterialButton(
                        this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        reset.setText(R.string.filter_reset);
        top.addView(reset);
        ScrollView scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout sections = new LinearLayout(this);
        sections.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(sections);
        MaterialButton results = new MaterialButton(this);
        results.setCornerRadius(dp(16));
        results.setInsetTop(0);
        results.setInsetBottom(0);
        LinearLayout.LayoutParams resultParams = new LinearLayout.LayoutParams(-1, dp(48));
        resultParams.topMargin = dp(12);
        root.addView(results, resultParams);
        Runnable refresh =
                () -> {
                    int count = 0;
                    for (Product product : products)
                        if (draft.matches(product, query, averageRatings)) count++;
                    results.setText(getString(R.string.filter_results, count));
                    results.setEnabled(draft.rating == 0 || ratingsLoaded);
                };
        Runnable render =
                () -> {
                    sections.removeAllViews();
                    addFilterChoices(
                            sections,
                            getString(R.string.filter_category),
                            getResources().getStringArray(R.array.filter_categories),
                            draft.category,
                            index -> {
                                draft.category = index;
                                refresh.run();
                            },
                            true);
                    addFilterChoices(
                            sections,
                            getString(R.string.price_filter_title),
                            getResources().getStringArray(R.array.price_preset_labels),
                            draft.price,
                            index -> {
                                draft.price = index;
                                refresh.run();
                            },
                            true);
                    addFilterChoices(
                            sections,
                            getString(R.string.filter_brand),
                            brandLabels,
                            brands.indexOf(draft.brand),
                            index -> {
                                draft.brand = brands.get(index);
                                refresh.run();
                            },
                            true);
                    int ratingIndex = draft.rating == 0 ? 0 : 5 - draft.rating;
                    addFilterChoices(
                            sections,
                            getString(R.string.filter_rating),
                            getResources().getStringArray(R.array.filter_rating_options),
                            ratingIndex,
                            index -> {
                                draft.rating = index == 0 ? 0 : 5 - index;
                                refresh.run();
                            },
                            ratingsLoaded);
                    if (!ratingsLoaded) {
                        TextView status = new TextView(this);
                        status.setText(R.string.filter_loading_ratings);
                        status.setId(R.id.filterRatingStatus);
                        status.setTextColor(getColor(R.color.text_secondary));
                        sections.addView(status);
                    }
                    addFilterChoices(
                            sections,
                            getString(R.string.filter_sort),
                            getResources().getStringArray(R.array.filter_sort_options),
                            draft.sort,
                            index -> {
                                draft.sort = index;
                                refresh.run();
                            },
                            true);
                    refresh.run();
                };
        reset.setOnClickListener(
                v -> {
                    draft.reset();
                    render.run();
                });
        results.setOnClickListener(
                v -> {
                    filters = draft.copy();
                    apply.run();
                    dialog.dismiss();
                });
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85f);
        dialog.setContentView(root, new ViewGroup.LayoutParams(-1, height));
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, insets) -> {
                    int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                    view.setPadding(dp(20), dp(16), dp(20), dp(16) + bottom);
                    return insets;
                });
        dialog.setOnShowListener(
                shown -> dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED));
        render.run();
        dialog.show();
        if (!ratingsLoaded)
            loadRatings(
                    products,
                    () -> {
                        apply.run();
                        if (dialog.isShowing()) render.run();
                    },
                    () -> {
                        TextView status = sections.findViewById(R.id.filterRatingStatus);
                        if (status != null) status.setText(R.string.filter_ratings_failed);
                    });
    }

    private Chip filterChip(String label) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColorResource(R.color.chip_background);
        chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text));
        chip.setChipStrokeColorResource(R.color.outline_soft);
        chip.setChipStrokeWidth(dp(1));
        chip.setEnsureMinTouchTargetSize(true);
        return chip;
    }

    private void addFilterChoices(
            LinearLayout parent,
            String title,
            String[] labels,
            int selected,
            IntConsumer onSelected,
            boolean enabled) {
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextSize(17);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(getColor(R.color.text_primary));
        heading.setPadding(0, dp(20), 0, dp(8));
        parent.addView(heading);
        ChipGroup choices = new ChipGroup(this);
        choices.setSingleSelection(true);
        choices.setSelectionRequired(true);
        choices.setChipSpacingHorizontal(dp(8));
        parent.addView(choices);
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Chip chip = filterChip(labels[i]);
            choices.addView(chip);
            chip.setChecked(i == selected);
            chip.setEnabled(enabled || i == 0);
            chip.setOnClickListener(v -> onSelected.accept(index));
        }
    }

    private void loadRatings(List<Product> products, Runnable success, Runnable failure) {
        int request = ++ratingRequest;
        List<Product> requested = new ArrayList<>(products);
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (Product product : requested)
            tasks.add(
                    FirebaseFirestore.getInstance()
                            .collection("products")
                            .document(product.getId())
                            .collection("reviews")
                            .get());
        Tasks.<QuerySnapshot>whenAllSuccess(tasks)
                .addOnSuccessListener(
                        snapshots -> {
                            if (request != ratingRequest || isDestroyed() || isFinishing()) return;
                            averageRatings.clear();
                            for (int i = 0; i < snapshots.size(); i++) {
                                double total = 0;
                                int count = 0;
                                for (DocumentSnapshot review : snapshots.get(i)) {
                                    Object rating = review.get("rating");
                                    if (rating instanceof Number) {
                                        double value = ((Number) rating).doubleValue();
                                        if (value >= 1 && value <= 5) {
                                            total += value;
                                            count++;
                                        }
                                    }
                                }
                                if (count > 0)
                                    averageRatings.put(requested.get(i).getId(), total / count);
                            }
                            ratingsLoaded = true;
                            success.run();
                        })
                .addOnFailureListener(
                        e -> {
                            if (request == ratingRequest && !isDestroyed() && !isFinishing())
                                failure.run();
                        });
    }

    private void openProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
    }

    private void showChangePasswordDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText oldPassword = content.findViewById(R.id.edtOldPassword);
        EditText newPassword = content.findViewById(R.id.edtNewPassword);
        EditText confirmPassword = content.findViewById(R.id.edtConfirmPassword);
        Button btnCancel = content.findViewById(R.id.btnCancelChangePassword);
        Button btnSave = content.findViewById(R.id.btnSaveChangePassword);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(content).create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        EditText[] fields = {oldPassword, newPassword, confirmPassword};
        Runnable unlock =
                () -> {
                    for (EditText field : fields) field.setEnabled(true);
                    btnSave.setEnabled(true);
                    btnSave.setText(R.string.label_doi_mat_khau);
                    btnCancel.setEnabled(true);
                    dialog.setCancelable(true);
                };
        btnSave.setOnClickListener(
                v -> {
                    for (EditText field : fields) InputValidator.error(field, null);
                    String oldValue = oldPassword.getText().toString();
                    String newValue = newPassword.getText().toString();
                    String confirmValue = confirmPassword.getText().toString();
                    if (oldValue.isEmpty()) {
                        InputValidator.error(
                                oldPassword, getString(R.string.password_current_required));
                        oldPassword.requestFocus();
                        return;
                    }
                    if (newValue.length() < 6 || newValue.length() > 128) {
                        InputValidator.error(newPassword, getString(R.string.auth_password_length));
                        newPassword.requestFocus();
                        return;
                    }
                    if (!newValue.equals(confirmValue)) {
                        InputValidator.error(
                                confirmPassword, getString(R.string.auth_password_mismatch));
                        confirmPassword.requestFocus();
                        return;
                    }
                    FirebaseUser user = AccountAuth.auth().getCurrentUser();
                    if (user == null || user.getEmail() == null) {
                        dialog.dismiss();
                        logout();
                        return;
                    }
                    for (EditText field : fields) field.setEnabled(false);
                    btnSave.setEnabled(false);
                    btnSave.setText(R.string.label_dang_xu_ly);
                    btnCancel.setEnabled(false);
                    dialog.setCancelable(false);
                    user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), oldValue))
                            .addOnSuccessListener(
                                    checked ->
                                            user.updatePassword(newValue)
                                                    .addOnSuccessListener(
                                                            done -> {
                                                                Toast.makeText(
                                                                                this,
                                                                                R.string
                                                                                        .password_changed,
                                                                                Toast.LENGTH_LONG)
                                                                        .show();
                                                                dialog.dismiss();
                                                                logout();
                                                            })
                                                    .addOnFailureListener(
                                                            error -> {
                                                                unlock.run();
                                                                if (error
                                                                        instanceof
                                                                        FirebaseAuthWeakPasswordException) {
                                                                    InputValidator.error(
                                                                            newPassword,
                                                                            AccountAuth.message(
                                                                                    error));
                                                                    newPassword.requestFocus();
                                                                } else
                                                                    Toast.makeText(
                                                                                    this,
                                                                                    AccountAuth
                                                                                            .message(
                                                                                                    error),
                                                                                    Toast
                                                                                            .LENGTH_LONG)
                                                                            .show();
                                                            }))
                            .addOnFailureListener(
                                    error -> {
                                        unlock.run();
                                        if (error
                                                instanceof
                                                FirebaseAuthInvalidCredentialsException) {
                                            InputValidator.error(
                                                    oldPassword,
                                                    getString(R.string.password_current_wrong));
                                            oldPassword.requestFocus();
                                        } else
                                            Toast.makeText(
                                                            this,
                                                            AccountAuth.message(error),
                                                            Toast.LENGTH_LONG)
                                                    .show();
                                    });
                });
        dialog.show();
        if (dialog.getWindow() != null)
            dialog.getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void showCartItems(LinearLayout container) {
        int request = ++cartRequest;
        container.removeAllViews();
        Button checkout = findViewById(R.id.btnCheckout);
        checkout.setOnClickListener(
                v -> {
                    ArrayList<String> selected = new ArrayList<>();
                    for (int i = 0; i < container.getChildCount(); i++) {
                        View row = container.getChildAt(i);
                        if (row.getTag() instanceof DocumentSnapshot
                                && !excludedCartItems.contains(
                                        ((DocumentSnapshot) row.getTag()).getId()))
                            selected.add(((DocumentSnapshot) row.getTag()).getId());
                    }
                    if (!selected.isEmpty())
                        startActivity(
                                new Intent(this, OrderActivity.class)
                                        .putStringArrayListExtra(
                                                OrderActivity.EXTRA_CART_IDS, selected));
                });
        checkout.setEnabled(false);
        MaterialCheckBox selectAll = findViewById(R.id.cartSelectAll);
        selectAll.setEnabled(false);
        selectAll.setOnClickListener(
                v -> {
                    boolean checked = selectAll.isChecked();
                    selectingAll = true;
                    for (int i = 0; i < container.getChildCount(); i++) {
                        View row = container.getChildAt(i);
                        if (row.getTag() instanceof DocumentSnapshot)
                            ((MaterialCheckBox) row.findViewById(R.id.cartSelected))
                                    .setChecked(checked);
                    }
                    selectingAll = false;
                    updateSelectedTotal(container, checkout);
                });
        Button clear = findViewById(R.id.btnClearCart);
        clear.setOnClickListener(
                v ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Xóa toàn bộ giỏ hàng?")
                                .setNegativeButton("Giữ lại", null)
                                .setPositiveButton(
                                        "Xóa tất cả",
                                        (dialog, which) -> {
                                            String id =
                                                    getSharedPreferences("session", MODE_PRIVATE)
                                                            .getString("userId", "");
                                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                                            db.collection("users")
                                                    .document(id)
                                                    .collection("cart")
                                                    .get()
                                                    .addOnSuccessListener(
                                                            items -> {
                                                                WriteBatch batch = db.batch();
                                                                for (DocumentSnapshot item : items)
                                                                    batch.delete(
                                                                            item.getReference());
                                                                batch.commit()
                                                                        .addOnSuccessListener(
                                                                                done ->
                                                                                        showCartItems(
                                                                                                container))
                                                                        .addOnFailureListener(
                                                                                error ->
                                                                                        Toast
                                                                                                .makeText(
                                                                                                        this,
                                                                                                        error
                                                                                                                .getMessage(),
                                                                                                        Toast
                                                                                                                .LENGTH_LONG)
                                                                                                .show());
                                                            })
                                                    .addOnFailureListener(
                                                            error ->
                                                                    Toast.makeText(
                                                                                    this,
                                                                                    error
                                                                                            .getMessage(),
                                                                                    Toast
                                                                                            .LENGTH_LONG)
                                                                            .show());
                                        })
                                .show());
        String userId = getSharedPreferences("session", MODE_PRIVATE).getString("userId", "");
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(
                        result -> {
                            if (request != cartRequest || isDestroyed() || isFinishing()) return;
                            container.removeAllViews();
                            List<DocumentSnapshot> validItems = new ArrayList<>();
                            for (DocumentSnapshot item : result) {
                                if (PurchaseItems.valid(item.getData())) validItems.add(item);
                            }
                            int itemCount = 0;
                            checkout.setEnabled(!validItems.isEmpty());
                            findViewById(R.id.cartSummary)
                                    .setVisibility(validItems.isEmpty() ? View.GONE : View.VISIBLE);
                            clear.setVisibility(result.size() > 1 ? View.VISIBLE : View.GONE);
                            findViewById(R.id.cartSelectionBar)
                                    .setVisibility(validItems.isEmpty() ? View.GONE : View.VISIBLE);
                            selectAll.setEnabled(!validItems.isEmpty());
                            if (validItems.isEmpty()) {
                                View emptyState =
                                        getLayoutInflater()
                                                .inflate(R.layout.empty_cart, container, false);
                                emptyState
                                        .findViewById(R.id.btnEmptyBrowse)
                                        .setOnClickListener(
                                                v ->
                                                        ((BottomNavigationView)
                                                                        findViewById(R.id.tabBar))
                                                                .setSelectedItemId(
                                                                        R.id.nav_products));
                                container.addView(emptyState);
                                LinearLayout suggestions =
                                        emptyState.findViewById(R.id.emptyCartSuggestions);
                                FirebaseFirestore.getInstance()
                                        .collection("products")
                                        .limit(4)
                                        .get()
                                        .addOnSuccessListener(
                                                products -> {
                                                    for (DocumentSnapshot doc : products) {
                                                        String name = doc.getString("name");
                                                        Long price = doc.getLong("price");
                                                        if (name == null || price == null) continue;
                                                        Product product =
                                                                new Product(
                                                                        doc.getId(),
                                                                        name,
                                                                        price.intValue(),
                                                                        ProductImages.find(
                                                                                doc.getString(
                                                                                        "image")),
                                                                        doc.getString(
                                                                                "description"));
                                                        View tile =
                                                                ProductCards.create(
                                                                        this, suggestions, product);
                                                        LinearLayout.LayoutParams params =
                                                                new LinearLayout.LayoutParams(
                                                                        dp(176),
                                                                        ViewGroup.LayoutParams
                                                                                .WRAP_CONTENT);
                                                        params.setMarginEnd(dp(12));
                                                        suggestions.addView(tile, params);
                                                        tile.setOnClickListener(
                                                                v ->
                                                                        startActivity(
                                                                                ProductDetailActivity
                                                                                        .createIntent(
                                                                                                this,
                                                                                                product)));
                                                    }
                                                });
                            } else {
                                for (DocumentSnapshot c : validItems) {
                                    String name = c.getString("name");
                                    Long priceValue = c.getLong("price");
                                    Long quantityValue = c.getLong("quantity");
                                    int price = priceValue.intValue();
                                    int imageResource = ProductImages.find(c.getString("image"));
                                    int quantity = quantityValue.intValue();
                                    String productId = c.getId();
                                    itemCount += quantity;

                                    View row =
                                            getLayoutInflater()
                                                    .inflate(R.layout.item_cart, container, false);
                                    container.addView(row);
                                    row.setTag(c);
                                    MaterialCheckBox selected = row.findViewById(R.id.cartSelected);
                                    selected.setChecked(!excludedCartItems.contains(c.getId()));
                                    selected.setContentDescription("Chọn " + name);
                                    selected.setOnCheckedChangeListener(
                                            (box, checked) -> {
                                                if (checked) excludedCartItems.remove(c.getId());
                                                else excludedCartItems.add(c.getId());
                                                if (!selectingAll)
                                                    updateSelectedTotal(container, checkout);
                                            });
                                    ImageView image = row.findViewById(R.id.cartImage);
                                    image.setImageResource(imageResource);
                                    image.setContentDescription(name);
                                    ((TextView) row.findViewById(R.id.cartName)).setText(name);
                                    ((TextView) row.findViewById(R.id.cartPrice))
                                            .setText(
                                                    getString(
                                                            R.string.unit_price,
                                                            Money.format(price)));
                                    ((TextView) row.findViewById(R.id.cartLineTotal))
                                            .setText(Money.format((long) price * quantity));
                                    ((TextView) row.findViewById(R.id.cartQuantity))
                                            .setText(String.valueOf(quantity));
                                    Button minus = row.findViewById(R.id.cartMinus);
                                    Button plus = row.findViewById(R.id.cartPlus);
                                    Button remove = row.findViewById(R.id.cartRemove);
                                    minus.setOnClickListener(
                                            v -> changeCartQuantity(container, productId, -1));
                                    plus.setOnClickListener(
                                            v -> changeCartQuantity(container, productId, 1));
                                    remove.setOnClickListener(
                                            v ->
                                                    c.getReference()
                                                            .delete()
                                                            .addOnSuccessListener(
                                                                    done ->
                                                                            showCartItems(
                                                                                    container))
                                                            .addOnFailureListener(
                                                                    error ->
                                                                            Toast.makeText(
                                                                                            this,
                                                                                            error
                                                                                                    .getMessage(),
                                                                                            Toast
                                                                                                    .LENGTH_LONG)
                                                                                    .show()));
                                }
                            }
                            updateCartBadge(itemCount);
                            updateSelectedTotal(container, checkout);
                            ((TextView) findViewById(R.id.tvTitle))
                                    .setText(getString(R.string.cart_title, itemCount));
                        })
                .addOnFailureListener(
                        error ->
                                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void updateSelectedTotal(LinearLayout container, Button checkout) {
        long total = 0;
        int selected = 0, count = 0;
        for (int i = 0; i < container.getChildCount(); i++) {
            Object tag = container.getChildAt(i).getTag();
            if (tag instanceof DocumentSnapshot) {
                DocumentSnapshot item = (DocumentSnapshot) tag;
                count++;
                if (!excludedCartItems.contains(item.getId())) {
                    selected++;
                    total += PurchaseItems.lineTotal(item.getData());
                }
            }
        }
        ((TextView) findViewById(R.id.tvCartTotal))
                .setText(getString(R.string.total_price, Money.format(total)));
        checkout.setText(getString(R.string.checkout_selected_count, selected));
        checkout.setEnabled(selected > 0);
        ((MaterialCheckBox) findViewById(R.id.cartSelectAll))
                .setChecked(count > 0 && selected == count);
    }

    private void refreshCartBadge() {
        String userId = getSharedPreferences("session", MODE_PRIVATE).getString("userId", "");
        if (userId.isEmpty()) return;
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(
                        result -> {
                            int count = 0;
                            for (DocumentSnapshot item : result) {
                                Long quantity = item.getLong("quantity");
                                if (quantity != null) count += quantity.intValue();
                            }
                            updateCartBadge(count);
                        });
    }

    private void updateCartBadge(int count) {
        BottomNavigationView nav = findViewById(R.id.tabBar);
        if (count > 0) nav.getOrCreateBadge(R.id.nav_cart).setNumber(count);
        else nav.removeBadge(R.id.nav_cart);
    }

    private void showAccount(LinearLayout parent) {
        extraRequest++;
        parent.removeAllViews();
        View header = getLayoutInflater().inflate(R.layout.account_header, parent, false);
        header.setClipToOutline(true);
        TextView name = header.findViewById(R.id.accountName);
        TextView email = header.findViewById(R.id.accountEmail);
        TextView avatar = header.findViewById(R.id.accountAvatar);
        name.setText(username);
        avatar.setText(
                username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase(Locale.ROOT));
        header.setOnClickListener(v -> openProfile());
        parent.addView(header);
        email.setText(AccountAuth.currentEmail());
        accountAction(
                parent,
                getString(R.string.purchase_history),
                R.drawable.nav_cart,
                this::openHistory);
        accountAction(
                parent,
                "Sổ địa chỉ",
                R.drawable.ic_shipping,
                () -> startActivity(new Intent(this, AddressBookActivity.class)));
        accountAction(
                parent,
                "Sản phẩm yêu thích",
                R.drawable.ic_favorite,
                () -> startActivity(new Intent(this, FavoritesActivity.class)));
        accountAction(
                parent,
                getString(R.string.personal_information),
                R.drawable.nav_account,
                this::openProfile);
        accountAction(
                parent,
                getString(R.string.label_doi_mat_khau),
                R.drawable.ic_security,
                this::showChangePasswordDialog);
        accountAction(
                parent,
                getString(R.string.label_dang_xuat),
                R.drawable.ic_logout,
                this::confirmLogout);
    }

    private void openHistory() {
        Intent history = new Intent(this, OrderActivity.class);
        history.putExtra(OrderActivity.EXTRA_HISTORY, true);
        startActivity(history);
    }

    private void confirmLogout() {
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setBackground(ContextCompat.getDrawable(this, R.drawable.bg_surface))
                        .setTitle(R.string.logout_title)
                        .setMessage(R.string.logout_message)
                        .setNegativeButton(R.string.logout_stay, null)
                        .setPositiveButton(
                                R.string.label_dang_xuat, (confirmation, which) -> logout())
                        .create();
        dialog.setOnShowListener(
                shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFB3261E));
        dialog.show();
    }

    private void accountAction(
            LinearLayout parent, String label, int iconResource, Runnable action) {
        View row = getLayoutInflater().inflate(R.layout.item_account_action, parent, false);
        TextView title = row.findViewById(R.id.accountActionTitle);
        title.setText(label);
        ImageView icon = row.findViewById(R.id.accountActionIcon);
        icon.setImageResource(iconResource);
        if (iconResource == R.drawable.ic_logout) {
            title.setTextColor(0xFFB3261E);
            icon.setImageTintList(ColorStateList.valueOf(0xFFB3261E));
            row.findViewById(R.id.accountActionChevron).setVisibility(View.GONE);
        }
        parent.addView(row);
        row.setOnClickListener(v -> action.run());
    }

    private void showNotifications(LinearLayout parent) {
        int request = ++extraRequest;
        parent.removeAllViews();
        TextView loading = new TextView(this);
        loading.setText(R.string.label_dang_tai_thong_bao);
        parent.addView(loading);
        String id = getSharedPreferences("session", MODE_PRIVATE).getString("userId", "");
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(id)
                .collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(
                        result -> {
                            if (request != extraRequest || isDestroyed() || isFinishing()) return;
                            parent.removeAllViews();
                            if (result.isEmpty()) {
                                loading.setText(R.string.label_ban_chua_co_thong_bao_dat_hang);
                                parent.addView(loading);
                            }
                            for (DocumentSnapshot order : result) {
                                View noticeRow =
                                        getLayoutInflater()
                                                .inflate(R.layout.item_notice, parent, false);
                                String date =
                                        order.getDate("createdAt") == null
                                                ? ""
                                                : new SimpleDateFormat(
                                                                "dd/MM/yyyy · HH:mm",
                                                                Locale.getDefault())
                                                        .format(order.getDate("createdAt"));
                                ((TextView) noticeRow.findViewById(R.id.noticeMeta))
                                        .setText(
                                                getString(
                                                        R.string.notice_metadata,
                                                        date,
                                                        order.getId().length() > 8
                                                                ? order.getId().substring(0, 8)
                                                                        + "…"
                                                                : order.getId()));
                                ((TextView) noticeRow.findViewById(R.id.noticeTotal))
                                        .setText(
                                                Money.format(
                                                        order.getLong("total") == null
                                                                ? 0
                                                                : order.getLong("total")));
                                boolean read =
                                        Boolean.TRUE.equals(order.getBoolean("notificationRead"));
                                TextView state = noticeRow.findViewById(R.id.noticeState);
                                state.setText(read ? R.string.notice_read : R.string.notice_unread);
                                state.setTextColor(
                                        ContextCompat.getColor(
                                                this,
                                                read ? R.color.text_secondary : R.color.brand));
                                parent.addView(noticeRow);
                                noticeRow.setOnClickListener(
                                        v -> {
                                            order.getReference()
                                                    .update("notificationRead", true)
                                                    .addOnSuccessListener(
                                                            done -> {
                                                                state.setText(R.string.notice_read);
                                                                state.setTextColor(
                                                                        ContextCompat.getColor(
                                                                                this,
                                                                                R.color
                                                                                        .text_secondary));
                                                            })
                                                    .addOnFailureListener(
                                                            error ->
                                                                    Toast.makeText(
                                                                                    this,
                                                                                    "Không lưu được"
                                                                                        + " trạng"
                                                                                        + " thái đã"
                                                                                        + " xem",
                                                                                    Toast
                                                                                            .LENGTH_SHORT)
                                                                            .show());

                                            Intent history = new Intent(this, OrderActivity.class);
                                            history.putExtra(
                                                    OrderActivity.EXTRA_ORDER_ID, order.getId());
                                            startActivity(history);
                                        });
                            }
                        })
                .addOnFailureListener(
                        error ->
                                loading.setText(
                                        getString(
                                                R.string.notification_error, error.getMessage())));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void changeCartQuantity(LinearLayout container, String productId, int amount) {
        String userId = getSharedPreferences("session", MODE_PRIVATE).getString("userId", "");
        DocumentReference item =
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("cart")
                        .document(productId);
        FirebaseFirestore.getInstance()
                .runTransaction(
                        transaction -> {
                            DocumentSnapshot snapshot = transaction.get(item);
                            if (snapshot.exists()) {
                                Long savedQuantity = snapshot.getLong("quantity");
                                long quantity =
                                        (savedQuantity == null ? 0 : savedQuantity) + amount;
                                if (quantity <= 0) transaction.delete(item);
                                else transaction.update(item, "quantity", quantity);
                            }
                            return null;
                        })
                .addOnSuccessListener(done -> showCartItems(container))
                .addOnFailureListener(
                        error ->
                                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show());
    }
}
