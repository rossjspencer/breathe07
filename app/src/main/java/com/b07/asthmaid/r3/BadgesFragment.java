package com.b07.asthmaid.r3;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
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
        ImageView badge1Lock = view.findViewById(R.id.badge1Lock);

        LinearLayout badge2Layout = view.findViewById(R.id.badge2Layout);
        ImageView badge2Image = view.findViewById(R.id.badge2Image);
        ImageView badge2Lock = view.findViewById(R.id.badge2Lock);

        LinearLayout badge3Layout = view.findViewById(R.id.badge3Layout);
        ImageView badge3Image = view.findViewById(R.id.badge3Image);
        ImageView badge3Lock = view.findViewById(R.id.badge3Lock);

        statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(TEMP_USER_ID);

        // badges should be grayscale by default
        applyGrayscale(badge1Image);
        applyGrayscale(badge2Image);
        applyGrayscale(badge3Image);

        checkBadgeStatus(badge1Layout, badge1Image, badge1Lock,
                badge2Layout, badge2Image, badge2Lock,
                badge3Layout, badge3Image, badge3Lock);

        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        return view;
    }

    private void applyGrayscale(ImageView imageView) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        imageView.setColorFilter(filter);
    }

    private void checkBadgeStatus(LinearLayout b1Layout, ImageView b1Image, ImageView b1Lock,
                                  LinearLayout b2Layout, ImageView b2Image, ImageView b2Lock,
                                  LinearLayout b3Layout, ImageView b3Image, ImageView b3Lock) {
        
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GuideStats stats = snapshot.getValue(GuideStats.class);
                
                // badge 1 check
                if (stats != null && stats.totalSessions >= 10) {
                    unlockBadge(b1Layout, b1Image, b1Lock, R.drawable.badge_ten_sessions);
                }

                // badge 2 and 3 stuff goes here
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("Why does this keep happening!?!?");
            }
        });
    }

    private void unlockBadge(LinearLayout layout, ImageView image, ImageView lock, int drawableId) {
        layout.setAlpha(1.0f);
        image.clearColorFilter(); 
        image.setImageResource(drawableId);
        image.setBackgroundColor(0); 
        lock.setVisibility(View.GONE);
    }
}