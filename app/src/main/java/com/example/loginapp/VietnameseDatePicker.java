package com.example.loginapp;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class VietnameseDatePicker extends DialogFragment {
    static final String TAG = "history_date_picker";
    static final String RESULT = "history_date_result";
    private static final int SELECT_DAY = 0;
    private static final int SELECT_MONTH = 1;
    private static final int SELECT_YEAR = 2;
    private int year, month, day, stage, yearPage;
    private boolean selected = true;
    private long minimum, maximum;
    private LinearLayout body;
    private TextView preview, step;
    private MaterialButton yearButton, monthButton, previous, next;
    private AlertDialog dialog;

    static VietnameseDatePicker create(boolean from, long initial, long min, long max) {
        VietnameseDatePicker picker = new VietnameseDatePicker();
        Bundle args = new Bundle();
        args.putBoolean("from", from);
        args.putLong("initial", initial);
        args.putLong("min", min);
        args.putLong("max", max);
        picker.setArguments(args);
        return picker;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle state) {
        Bundle args = requireArguments();
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(args.getLong("initial"));
        year = state == null ? date.get(Calendar.YEAR) : state.getInt("year");
        month = state == null ? date.get(Calendar.MONTH) : state.getInt("month");
        day = state == null ? date.get(Calendar.DAY_OF_MONTH) : state.getInt("day");
        stage = state == null ? SELECT_DAY : state.getInt("stage");
        yearPage = state == null ? year / 12 * 12 : state.getInt("yearPage");
        selected = state == null || state.getBoolean("selected");
        minimum = args.getLong("min");
        maximum = args.getLong("max");
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(8));
        TextView title =
                label(
                        getString(
                                args.getBoolean("from")
                                        ? R.string.history_pick_from
                                        : R.string.history_pick_to),
                        16);
        root.addView(title);
        preview = label("", 26);
        preview.setTextColor(requireContext().getColor(R.color.brand));
        preview.setPadding(0, dp(12), 0, dp(16));
        root.addView(preview);
        LinearLayout navigation = new LinearLayout(requireContext());
        navigation.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(navigation);
        previous = button("‹", navigation, 0.5f);
        previous.setContentDescription(getString(R.string.calendar_previous));
        yearButton = button("", navigation, 1.2f);
        monthButton = button("", navigation, 1.4f);
        next = button("›", navigation, 0.5f);
        next.setContentDescription(getString(R.string.calendar_next));
        yearButton.setOnClickListener(
                v -> {
                    stage = SELECT_YEAR;
                    yearPage = year / 12 * 12;
                    render();
                });
        monthButton.setOnClickListener(
                v -> {
                    stage = SELECT_MONTH;
                    render();
                });
        previous.setOnClickListener(v -> move(-1));
        next.setOnClickListener(v -> move(1));
        step = label("", 16);
        step.setPadding(0, dp(12), 0, dp(8));
        root.addView(step);
        ScrollView scroll = new ScrollView(requireContext());
        int height =
                Math.min(dp(320), (int) (getResources().getDisplayMetrics().heightPixels * 0.42f));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, height));
        body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setBackground(
                                AppCompatResources.getDrawable(
                                        requireContext(), R.drawable.bg_surface))
                        .setView(root)
                        .setNegativeButton(R.string.action_cancel, null)
                        .setPositiveButton(
                                R.string.history_pick_date,
                                (d, w) -> {
                                    Bundle result = new Bundle();
                                    result.putBoolean("from", args.getBoolean("from"));
                                    result.putLong(
                                            "day", ShoppingFilters.dayStart(year, month, day));
                                    getParentFragmentManager().setFragmentResult(RESULT, result);
                                })
                        .create();
        dialog.setOnShowListener(shown -> render());
        return dialog;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView label(String value, int size) {
        TextView text = new TextView(requireContext());
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(requireContext().getColor(R.color.text_primary));
        return text;
    }

    private MaterialButton button(String text, LinearLayout row, float weight) {
        MaterialButton button =
                new MaterialButton(
                        requireContext(),
                        null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(text);
        button.setTextSize(14);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(0, 0, 0, 0);
        button.setCornerRadius(dp(12));
        button.setStrokeWidth(0);
        row.addView(button, new LinearLayout.LayoutParams(0, dp(48), weight));
        return button;
    }

    private boolean allowed(int year, int month, int day) {
        long value = ShoppingFilters.dayStart(year, month, day);
        return value >= minimum && value <= maximum;
    }

    private void render() {
        body.removeAllViews();
        String[] months = getResources().getStringArray(R.array.calendar_months);
        preview.setText(
                new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
                        .format(new Date(ShoppingFilters.dayStart(year, month, day))));
        yearButton.setText(String.valueOf(year));
        monthButton.setText(months[month]);
        step.setText(
                stage == SELECT_YEAR
                        ? R.string.calendar_year
                        : stage == SELECT_MONTH
                                ? R.string.calendar_month
                                : R.string.history_pick_date);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setEnabled(stage == SELECT_DAY && selected && allowed(year, month, day));
        previous.setEnabled(
                stage == SELECT_YEAR
                        ? yearPage > 1900
                        : stage == SELECT_MONTH ? year > 1900 : year > 1900 || month > 0);
        next.setEnabled(
                stage == SELECT_YEAR
                        ? yearPage + 11 < 2100
                        : stage == SELECT_MONTH ? year < 2100 : year < 2100 || month < 11);
        if (stage == SELECT_YEAR) {
            for (int row = 0; row < 4; row++) {
                LinearLayout line = row();
                for (int column = 0; column < 3; column++) {
                    final int value = yearPage + row * 3 + column;
                    MaterialButton choice = button(String.valueOf(value), line, 1);
                    boolean inRange =
                            value >= 1900
                                    && value <= 2100
                                    && ShoppingFilters.dayStart(value, 11, 31) >= minimum
                                    && ShoppingFilters.dayStart(value, 0, 1) <= maximum;
                    choice.setEnabled(inRange);
                    highlight(choice, value == year);
                    choice.setOnClickListener(
                            v -> {
                                year = value;
                                clampDay();
                                stage = SELECT_MONTH;
                                selected = false;
                                render();
                            });
                }
            }
        } else if (stage == SELECT_MONTH) {
            for (int row = 0; row < 4; row++) {
                LinearLayout line = row();
                for (int column = 0; column < 3; column++) {
                    final int value = row * 3 + column;
                    MaterialButton choice = button(months[value], line, 1);
                    choice.setEnabled(
                            ShoppingFilters.dayStart(
                                                    year,
                                                    value,
                                                    ShoppingFilters.daysInMonth(year, value))
                                            >= minimum
                                    && ShoppingFilters.dayStart(year, value, 1) <= maximum);
                    highlight(choice, value == month);
                    choice.setOnClickListener(
                            v -> {
                                month = value;
                                clampDay();
                                stage = SELECT_DAY;
                                selected = false;
                                render();
                            });
                }
            }
        } else {
            LinearLayout weekdays = row();
            for (String weekday : getResources().getStringArray(R.array.calendar_weekdays)) {
                TextView label = label(weekday, 13);
                label.setGravity(Gravity.CENTER);
                weekdays.addView(label, new LinearLayout.LayoutParams(0, dp(28), 1));
            }
            Calendar first = Calendar.getInstance();
            first.setTimeInMillis(ShoppingFilters.dayStart(year, month, 1));
            int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            int days = ShoppingFilters.daysInMonth(year, month);
            for (int start = 0; start < offset + days; start += 7) {
                LinearLayout line = row();
                for (int column = 0; column < 7; column++) {
                    final int value = start + column - offset + 1;
                    MaterialButton choice =
                            button(value < 1 || value > days ? "" : String.valueOf(value), line, 1);
                    if (value < 1 || value > days) choice.setVisibility(View.INVISIBLE);
                    else {
                        choice.setEnabled(allowed(year, month, value));
                        highlight(choice, selected && value == day);
                        choice.setOnClickListener(
                                v -> {
                                    day = value;
                                    selected = true;
                                    render();
                                });
                    }
                }
            }
        }
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(requireContext());
        body.addView(row);
        return row;
    }

    private void highlight(MaterialButton button, boolean active) {
        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        active ? requireContext().getColor(R.color.brand) : Color.TRANSPARENT));
        button.setTextColor(active ? Color.WHITE : requireContext().getColor(R.color.text_primary));
        if (!button.isEnabled()) button.setAlpha(0.35f);
    }

    private void clampDay() {
        day = Math.min(day, ShoppingFilters.daysInMonth(year, month));
    }

    private void move(int direction) {
        if (stage == SELECT_YEAR) yearPage += direction * 12;
        else if (stage == SELECT_MONTH) {
            year += direction;
            clampDay();
        } else {
            Calendar date = Calendar.getInstance();
            date.set(year, month, 1);
            date.add(Calendar.MONTH, direction);
            year = date.get(Calendar.YEAR);
            month = date.get(Calendar.MONTH);
            clampDay();
            selected = false;
        }
        render();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        state.putInt("year", year);
        state.putInt("month", month);
        state.putInt("day", day);
        state.putInt("stage", stage);
        state.putInt("yearPage", yearPage);
        state.putBoolean("selected", selected);
        super.onSaveInstanceState(state);
    }
}
