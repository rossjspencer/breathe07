package com.example.smartair;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
        Context context = holder.itemView.getContext();

        // 1. Set Name
        String fullName = child.firstName + " " + (child.lastName != null ? child.lastName : "");
        holder.tvName.setText(fullName);

        // 2. Set Status Bar Logic
        int score = child.asthmaScore;
        holder.progressBar.setProgress(score);

        // Color Logic (Green > 80, Yellow > 50, Red < 50)
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

        // 3. BUTTON CLICK LISTENERS

        // Manage: Edit Profile / Delete Child
        holder.btnManage.setOnClickListener(v -> {
            Intent intent = new Intent(context, ManageChildActivity.class);
            intent.putExtra("CHILD_ID", child.userId);
            context.startActivity(intent);
        });

        // Log In: Impersonate Child / Go to Dashboard
        holder.btnLoginAs.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChildHomeActivity.class);
            intent.putExtra("CHILD_ID", child.userId);
            context.startActivity(intent);
        });

        // Share: Configure Data Sharing Toggles
        holder.btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShareProviderActivity.class);
            intent.putExtra("CHILD_ID", child.userId);
            context.startActivity(intent);
        });

        // Invite: Generate Provider Code
        holder.btnInvite.setOnClickListener(v -> {
            Intent intent = new Intent(context, InviteProviderActivity.class);
            intent.putExtra("CHILD_ID", child.userId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return childList.size();
    }

    public static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        ProgressBar progressBar;
        Button btnManage, btnLoginAs, btnShare, btnInvite;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvChildName);
            tvStatus = itemView.findViewById(R.id.tvStatusText);
            progressBar = itemView.findViewById(R.id.pbHealthStatus);

            // Buttons
            btnManage = itemView.findViewById(R.id.btnManageChild);
            btnLoginAs = itemView.findViewById(R.id.btnLoginAsChild);
            btnShare = itemView.findViewById(R.id.btnShareChild);
            btnInvite = itemView.findViewById(R.id.btnInviteProvider);
        }
    }
}