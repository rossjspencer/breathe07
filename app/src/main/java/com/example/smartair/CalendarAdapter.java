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
            // 1) Real blank cell used for grid padding
            if (day.dayOfMonth == 0) {
                tvDayNum.setText("");
                tvMissedCount.setVisibility(View.GONE);
                itemView.setBackgroundResource(0);
                itemView.setAlpha(0.2f);
                return;
            }

            // Always show the day number for real days
            tvDayNum.setText(String.valueOf(day.dayOfMonth));

            // 2) Future day (we used -2)
            if (day.missedCount < 0) {
                // muted look, no missed chip
                tvMissedCount.setVisibility(View.GONE);
                itemView.setBackgroundResource(R.drawable.calendar_day_bg);
                itemView.setAlpha(0.5f);
                return;
            }

            // 3) Past/today with data
            itemView.setAlpha(1f);
            if (day.missedCount == 0) {
                itemView.setBackgroundResource(R.drawable.calendar_day_good);
                tvMissedCount.setVisibility(View.GONE);
            } else {
                itemView.setBackgroundResource(R.drawable.calendar_day_bg);
                tvMissedCount.setVisibility(View.VISIBLE);
                tvMissedCount.setText("-" + day.missedCount);
            }
        }

    }
}