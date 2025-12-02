package com.example.smartair;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private final List<CalendarDay> days;

    public CalendarAdapter(List<CalendarDay> days) {
        this.days = days;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.bind(day);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNum;
        TextView tvMissedCount;

        ViewHolder(View itemView) {
            super(itemView);
            tvDayNum = itemView.findViewById(R.id.tvDayNum);
            tvMissedCount = itemView.findViewById(R.id.tvMissedCount);
        }

        void bind(CalendarDay day) {
            if (day.missedCount < 0) {
                // for future days
                tvDayNum.setText("");
                tvMissedCount.setVisibility(View.GONE);
                itemView.setBackground(null);
            } else {
                tvDayNum.setText(String.valueOf(day.dayOfMonth));
                
                if (day.missedCount == 0) {
                    // good day
                    itemView.setBackgroundResource(R.drawable.calendar_day_good);
                    tvMissedCount.setVisibility(View.GONE);
                } else {
                    // missed day
                    itemView.setBackgroundResource(R.drawable.calendar_day_bg);
                    tvMissedCount.setVisibility(View.VISIBLE);
                    tvMissedCount.setText("-" + day.missedCount);
                }
            }
        }
    }
}