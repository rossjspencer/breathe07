package com.example.smartair;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {

    private ArrayList<User> childList;

    public ChildAdapter(ArrayList<User> childList) {
        this.childList = childList;
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_linked_child, parent, false);
        return new ChildViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        User child = childList.get(position);

        String fullName = child.firstName + " " + (child.lastName != null ? child.lastName : "");
        holder.tvName.setText(fullName);

        int score = child.asthmaScore;
        holder.progressBar.setProgress(score);

        // Color Logic: Green >= 80, Yellow >= 50, Red < 50
        if (score >= 80) {
            holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            holder.tvStatus.setText("Green Zone (" + score + "%)");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else if (score >= 50) {
            holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FFC107")));
            holder.tvStatus.setText("Yellow Zone (" + score + "%)");
            holder.tvStatus.setTextColor(Color.parseColor("#FFC107"));
        } else {
            holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
            holder.tvStatus.setText("Red Zone (" + score + "%)");
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"));
        }
    }

    @Override
    public int getItemCount() {
        return childList.size();
    }

    public static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        ProgressBar progressBar;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvChildName);
            tvStatus = itemView.findViewById(R.id.tvStatusText);
            progressBar = itemView.findViewById(R.id.pbHealthStatus);
        }
    }
}