package com.example.smartair.r3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartair.R;
import java.util.ArrayList;

public class LogDisplayHandler extends RecyclerView.Adapter<LogDisplayHandler.ViewHolder> {

    private ArrayList<MedicineLogEntry> entries = new ArrayList<>();
    private final MedicineLogFragment fragment;

    public LogDisplayHandler(MedicineLogFragment fragment) {
        this.fragment = fragment;
    }

    // shows new list
    public void setEntries(ArrayList<? extends MedicineLogEntry> newEntries) {
        entries = new ArrayList<>(newEntries); // copy list
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_medicine_log_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {
        MedicineLogEntry entry = entries.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView timestampView;
        TextView doseView;
        TextView nameView;
        ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampView = itemView.findViewById(R.id.tvTimestamp);
            doseView = itemView.findViewById(R.id.tvDose);
            nameView = itemView.findViewById(R.id.tvName);
            deleteButton = itemView.findViewById(R.id.btnDelete);
        }

        void bind(MedicineLogEntry entry) {
            timestampView.setText(entry.timestamp);
            doseView.setText(String.valueOf(entry.doseCount));
            nameView.setText(entry.name);

            deleteButton.setOnClickListener(v -> {
                fragment.showDeleteConfirmDialog(entry);
            });
        }
    }
}