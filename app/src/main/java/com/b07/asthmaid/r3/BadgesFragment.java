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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BadgesFragment extends Fragment {

    private static final String TEMP_USER_ID = "testUserId";
    private DatabaseReference statsRef;
    private DatabaseReference logsRef;

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
        logsRef = FirebaseDatabase.getInstance().getReference("medicine_logs").child("rescue").child(TEMP_USER_ID);

        // Initial grayscale application
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
        
        // check guide stats
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GuideStats stats = snapshot.getValue(GuideStats.class);
                
                // badge 1 check (10 Sessions)
                if (stats != null && stats.totalSessions >= 10) {
                    unlockBadge(b1Layout, b1Image, b1Lock, R.drawable.badge_ten_sessions);
                }

                // Badge 2 (Placeholder) - Unobtainable for now
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("Why does this keep happening!?!?");
            }
        });

        // badge 3 check
        checkRescueUsage(b3Layout, b3Image, b3Lock);
    }

    private void checkRescueUsage(LinearLayout layout, ImageView image, ImageView lock) {
        logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            //TODO: MAKE THRESHOLDS CONFIGURABLE
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int countLast30Days = 0;
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -30);
                Date thirtyDaysAgo = cal.getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                for (DataSnapshot child : snapshot.getChildren()) {
                    RescueLogEntry entry = child.getValue(RescueLogEntry.class);
                    if (entry != null && entry.timestamp != null) {
                        try {
                            Date entryDate = sdf.parse(entry.timestamp);
                            if (entryDate != null && entryDate.after(thirtyDaysAgo)) {
                                countLast30Days++;
                            }
                        } catch (ParseException e) {
                            // ignore parse errors
                        }
                    }
                }

                // <= 4 uses in 30 days earns the badge
                if (countLast30Days <= 4) {
                    unlockBadge(layout, image, lock, R.drawable.badge_low_rescue_month);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // handle error
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