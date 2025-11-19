package com.b07.asthmaid.r3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.b07.asthmaid.R;
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
                .inflate(R.layout.item_medicine_log_entry, parent, false);
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
        TextView typeView;
        ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampView = itemView.findViewById(R.id.tvTimestamp);
            doseView = itemView.findViewById(R.id.tvDose);
            typeView = itemView.findViewById(R.id.tvType);
            deleteButton = itemView.findViewById(R.id.btnDelete);
        }

        void bind(MedicineLogEntry entry) {
            timestampView.setText(entry.timestamp);
            doseView.setText(String.valueOf(entry.doseCount));

            if (entry instanceof ControllerLogEntry) {
                typeView.setText("Controller");
            } else if (entry instanceof RescueLogEntry) {
                typeView.setText("Rescue");
            } else {
                typeView.setText("Unknown");
            }

            deleteButton.setOnClickListener(v -> {
                fragment.showDeleteConfirmDialog(entry);
            });
        }
    }
}