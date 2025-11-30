package com.example.smartair;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<SymptomLog> logList;

    public HistoryAdapter(List<SymptomLog> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_log, parent, false);
        return new HistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SymptomLog log = logList.get(position);

        // Date Format
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(log.timestamp)));

        // Logged By
        holder.tvLoggedBy.setText("Logged by: " + (log.loggedBy != null ? log.loggedBy : "Unknown"));

        // Severity
        holder.tvSeverity.setText("Severity: " + log.severity + "/10");
        if (log.severity >= 7) holder.tvSeverity.setTextColor(Color.RED);
        else if (log.severity >= 4) holder.tvSeverity.setTextColor(Color.parseColor("#FF9800")); // Orange
        else holder.tvSeverity.setTextColor(Color.parseColor("#4CAF50")); // Green

        // Symptoms Summary
        if (log.symptoms != null && !log.symptoms.isEmpty()) {
            holder.tvSymptoms.setText("Symptoms: " + String.join(", ", log.symptoms));
            holder.tvSymptoms.setVisibility(View.VISIBLE);
        } else {
            holder.tvSymptoms.setVisibility(View.GONE);
        }

        // Triggers Summary
        if (log.triggers != null && !log.triggers.isEmpty()) {
            holder.tvTriggers.setText("Triggers: " + String.join(", ", log.triggers));
            holder.tvTriggers.setVisibility(View.VISIBLE);
        } else {
            holder.tvTriggers.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvLoggedBy, tvSeverity, tvSymptoms, tvTriggers;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvLogDate);
            tvLoggedBy = itemView.findViewById(R.id.tvLoggedBy);
            tvSeverity = itemView.findViewById(R.id.tvSeverity);
            tvSymptoms = itemView.findViewById(R.id.tvSymptoms);
            tvTriggers = itemView.findViewById(R.id.tvTriggers);
        }
    }
}