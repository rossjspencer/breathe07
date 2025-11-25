package com.b07.asthmaid.r3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.b07.asthmaid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BadgesFragment extends Fragment {

    private static final String TEMP_USER_ID = "testUserId";
    private DatabaseReference statsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_badges, container, false);

        Button backButton = view.findViewById(R.id.badgesBackButton);
        
        LinearLayout badge1Layout = view.findViewById(R.id.badge1Layout);
        ImageView badge1Image = view.findViewById(R.id.badge1Image);

        statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(TEMP_USER_ID);

        checkBadgeStatus(badge1Layout, badge1Image);

        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        return view;
    }

    private void checkBadgeStatus(LinearLayout badgeLayout, ImageView badgeImage) {
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GuideStats stats = snapshot.getValue(GuideStats.class);
                if (stats != null && stats.totalSessions >= 10) {
                    // unlock badge
                    badgeLayout.setAlpha(1.0f);
                    badgeImage.setImageResource(android.R.drawable.star_big_on);
                    badgeImage.setBackgroundColor(0); // get rid of background
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("Why does this keep happening!?!?");
            }
        });
    }
}