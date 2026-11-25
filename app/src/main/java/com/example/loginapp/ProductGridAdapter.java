package com.example.loginapp;

import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ProductGridAdapter extends RecyclerView.Adapter<ProductGridAdapter.Holder> {
    private List<Product> products;
    private final View header;
    private final Consumer<Product> openProduct;

    public ProductGridAdapter(
            RecyclerView grid, View header, List<Product> products, Consumer<Product> openProduct) {
        this.products = new ArrayList<>(products);
        this.header = header;
        this.openProduct = openProduct;
        GridLayoutManager layout = (GridLayoutManager) grid.getLayoutManager();
        layout.setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return position == 0 ? 2 : 1;
                    }
                });
        grid.setItemAnimator(null);
        int gap = Math.round(12 * grid.getResources().getDisplayMetrics().density);
        grid.addItemDecoration(
                new RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(
                            @NonNull Rect out,
                            @NonNull View view,
                            @NonNull RecyclerView parent,
                            @NonNull RecyclerView.State state) {
                        int position = parent.getChildAdapterPosition(view);
                        if (position <= 0) return;
                        int column = (position - 1) % 2;
                        out.set(column == 0 ? 0 : gap / 2, gap, column == 0 ? gap / 2 : 0, 0);
                    }
                });
    }

    public void submitProducts(List<Product> visible) {
        List<Product> next = new ArrayList<>(visible);
        DiffUtil.DiffResult changes =
                DiffUtil.calculateDiff(
                        new DiffUtil.Callback() {
                            @Override
                            public int getOldListSize() {
                                return products.size() + 1;
                            }

                            @Override
                            public int getNewListSize() {
                                return next.size() + 1;
                            }

                            @Override
                            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                                if (oldPosition == 0 || newPosition == 0)
                                    return oldPosition == newPosition;
                                return products.get(oldPosition - 1)
                                        .getId()
                                        .equals(next.get(newPosition - 1).getId());
                            }

                            @Override
                            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                                return oldPosition == 0
                                        || products.get(oldPosition - 1)
                                                == next.get(newPosition - 1);
                            }
                        });
        products = next;
        changes.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemCount() {
        return products.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        if (type == 0) return new Holder(header);
        Holder holder =
                new Holder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_product, parent, false));
        holder.itemView.setOnClickListener(
                v -> {
                    int position = holder.getBindingAdapterPosition();
                    if (position > 0 && position <= products.size())
                        openProduct.accept(products.get(position - 1));
                });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (position > 0) ProductCards.bind(holder.itemView, products.get(position - 1));
    }

    static class Holder extends RecyclerView.ViewHolder {
        Holder(View view) {
            super(view);
        }
    }
}
