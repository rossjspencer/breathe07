package com.example.smartair;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AdherenceAdapter extends RecyclerView.Adapter<AdherenceAdapter.ViewHolder> {

    private final List<AdherenceDay> days;

    public AdherenceAdapter(List<AdherenceDay> days) {
        this.days = days;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_adherence_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdherenceDay day = days.get(position);
        holder.bind(day);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName;
        ImageView imgStatus;
        TextView tvDifference;

        ViewHolder(View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            imgStatus = itemView.findViewById(R.id.imgStatus);
            tvDifference = itemView.findViewById(R.id.tvDifference);
        }

        void bind(AdherenceDay day) {
            tvDayName.setText(day.dayName);
            
            if (day.isFuture) {
                itemView.setAlpha(0.4f); // Dim future days
                tvDayName.setTextColor(Color.BLACK); 
                imgStatus.setVisibility(View.GONE);
                tvDifference.setVisibility(View.GONE);
            } else {
                itemView.setAlpha(1.0f); // Reset alpha
                tvDayName.setTextColor(Color.BLACK);
                if (day.compliant) {
                    imgStatus.setImageResource(R.drawable.status_success);
                    imgStatus.setColorFilter(null); // Clear any filter
                    imgStatus.setVisibility(View.VISIBLE);
                    tvDifference.setVisibility(View.GONE);
                } else {
                    imgStatus.setVisibility(View.GONE);
                    tvDifference.setVisibility(View.VISIBLE);
                    String label = day.missedCount == 1 ? "dose" : "doses";
                    tvDifference.setText("Missed " + day.missedCount + " " + label);
                }
            }
        }
    }
}