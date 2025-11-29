package com.b07.asthmaid.r3;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.b07.asthmaid.R;

import java.util.ArrayList;
import java.util.List;

public class InventoryDisplayHandler extends RecyclerView.Adapter<InventoryDisplayHandler.ViewHolder> {

    private final ArrayList<InventoryItem> items = new ArrayList<>();
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(InventoryItem item);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setItems(List<InventoryItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InventoryDisplayHandler.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_inventory_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull InventoryDisplayHandler.ViewHolder holder,
            int position) {
        holder.bind(items.get(position), onDeleteClickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView nameView;
        final TextView percentView;
        final TextView expiryView;
        final ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.tvInventoryName);
            percentView = itemView.findViewById(R.id.tvInventoryPercent);
            expiryView = itemView.findViewById(R.id.tvInventoryExpiry);
            deleteButton = itemView.findViewById(R.id.btnDeleteInventory);
        }

        void bind(InventoryItem item, OnDeleteClickListener listener) {
            nameView.setText(item.name);
            percentView.setText(item.percentLeft + "%");
            expiryView.setText(item.expiryDate == null ? "" : item.expiryDate);

            int color = Color.BLACK;

            //expired/empty items are red, low items are orange
            if (item.isExpired()) {
                color = Color.RED;
            } else if (item.percentLeft <= 0) {
                 color = Color.RED;
            } else if (item.isLow()) {
                color = Color.parseColor("#FFA500"); // orange
            }

            nameView.setTextColor(color);
            percentView.setTextColor(color);
            expiryView.setTextColor(color);

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(item);
                }
            });
        }
    }
}