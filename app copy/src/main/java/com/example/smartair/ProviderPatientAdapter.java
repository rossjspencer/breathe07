package com.example.smartair;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ProviderPatientAdapter extends RecyclerView.Adapter<ProviderPatientAdapter.ViewHolder> {
    private ArrayList<User> list;
    public ProviderPatientAdapter(ArrayList<User> list) { this.list = list; }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_provider_patient, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User patient = list.get(position);
        holder.tvName.setText(patient.firstName + " " + patient.lastName);

        holder.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ProviderViewPatientActivity.class);
            intent.putExtra("CHILD_ID", patient.userId);
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName; Button btnView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPatientName);
            btnView = itemView.findViewById(R.id.btnViewRecords);
        }
    }
}