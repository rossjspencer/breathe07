package com.b07.asthmaid.r3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.b07.asthmaid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class BadgesFragment extends Fragment {

    private static final String TEMP_USER_ID = "testUserId";
    private static final String BADGE_CHANNEL_ID = "badge_alerts";
    
    private DatabaseReference statsRef;
    private DatabaseReference logsRef;
    private DatabaseReference settingsRef;

    // Defaults
    private int badge1Threshold = 10;
    private int badge3Threshold = 4;

    private RelativeLayout overlayLayout;
    private ImageView overlayBadgeImage;
    private TextView overlayBadgeText;
    private KonfettiView konfettiViewBadge;
    
    private TextView badge1Desc;
    private TextView badge3Desc;
    private TextView textCurrentStreak;
    
    private ProgressBar loadingSpinner;
    private ScrollView badgesScrollView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_badges, container, false);

        createNotificationChannel();

        Button backButton = view.findViewById(R.id.badgesBackButton);
        textCurrentStreak = view.findViewById(R.id.textCurrentStreak);
        textCurrentStreak.setVisibility(View.INVISIBLE); // hide initially
        
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        badgesScrollView = view.findViewById(R.id.badgesScrollView);
        
        LinearLayout badge1Layout = view.findViewById(R.id.badge1Layout);
        FrameLayout badge1Frame = view.findViewById(R.id.badge1Frame); 
        ImageView badge1Image = view.findViewById(R.id.badge1Image);
        ImageView badge1Lock = view.findViewById(R.id.badge1Lock);
        badge1Desc = view.findViewById(R.id.badge1Desc);

        LinearLayout badge2Layout = view.findViewById(R.id.badge2Layout);
        // FrameLayout badge2Frame = view.findViewById(R.id.badge2Frame); 
        ImageView badge2Image = view.findViewById(R.id.badge2Image);
        ImageView badge2Lock = view.findViewById(R.id.badge2Lock);

        LinearLayout badge3Layout = view.findViewById(R.id.badge3Layout);
        FrameLayout badge3Frame = view.findViewById(R.id.badge3Frame);
        ImageView badge3Image = view.findViewById(R.id.badge3Image);
        ImageView badge3Lock = view.findViewById(R.id.badge3Lock);
        badge3Desc = view.findViewById(R.id.badge3Desc);

        if (getActivity() != null) {
            overlayLayout = getActivity().findViewById(R.id.badgeOverlayLayout);
            overlayBadgeImage = getActivity().findViewById(R.id.mainOverlayBadgeImage);
            overlayBadgeText = getActivity().findViewById(R.id.mainOverlayBadgeText);
            konfettiViewBadge = getActivity().findViewById(R.id.mainKonfettiView);
        }

        statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(TEMP_USER_ID);
        logsRef = FirebaseDatabase.getInstance().getReference("medicine_logs").child("rescue").child(TEMP_USER_ID);
        settingsRef = FirebaseDatabase.getInstance().getReference("badge_settings").child(TEMP_USER_ID);

        // initial grayscale application
        applyGrayscale(badge1Image);
        applyGrayscale(badge2Image);
        applyGrayscale(badge3Image);

        // setup lock click listeners for animation
        setupBadgeClick(badge1Frame, badge1Image, badge1Lock, "Beginner Breather", R.drawable.badge_ten_sessions);
        setupBadgeClick(badge3Frame, badge3Image, badge3Lock, "Well Controlled", R.drawable.badge_low_rescue_month);

        // load settings first, then check status
        loadSettingsAndCheckStatus(badge1Frame, badge1Image, badge1Lock,
                null, badge2Image, badge2Lock,
                badge3Frame, badge3Image, badge3Lock, textCurrentStreak);

        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        return view;
    }
    
    private void loadSettingsAndCheckStatus(View b1Layout, ImageView b1Image, ImageView b1Lock,
                                            View b2Layout, ImageView b2Image, ImageView b2Lock,
                                            View b3Layout, ImageView b3Image, ImageView b3Lock,
                                            TextView streakText) {
        settingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer b1 = snapshot.child("badge1_threshold").getValue(Integer.class);
                Integer b3 = snapshot.child("badge3_threshold").getValue(Integer.class);
                
                if (b1 != null) badge1Threshold = b1;
                if (b3 != null) badge3Threshold = b3;
                
                // update UI descriptions
                if (badge1Desc != null) {
                    badge1Desc.setText("Complete " + badge1Threshold + " high-quality inhaler doses");
                }
                if (badge3Desc != null) {
                    badge3Desc.setText("Use your Rescue inhaler " + badge3Threshold + " or fewer times in 30 days");
                }
                
                // Now check status with loaded values
                checkBadgeStatus(b1Layout, b1Image, b1Lock, b2Layout, b2Image, b2Lock, b3Layout, b3Image, b3Lock, streakText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // If failed, proceed with defaults
                checkBadgeStatus(b1Layout, b1Image, b1Lock, b2Layout, b2Image, b2Lock, b3Layout, b3Image, b3Lock, streakText);
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Badge Alerts";
            String description = "Notifications for earned badges";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(BADGE_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (overlayLayout != null) {
            overlayLayout.setVisibility(View.GONE);
        }
    }

    private void applyGrayscale(ImageView imageView) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        imageView.setColorFilter(filter);
    }

    private void setupBadgeClick(View layout, ImageView image, ImageView lock, String name, int drawableId) {
        layout.setClickable(true);
        
        layout.setOnClickListener(v -> {
            Object tag = layout.getTag();
            // check if "ready to unlock" using tag set in checkBadgeStatus
            if ("READY".equals(tag)) {
                // pretty self explanatory bro
                showBadgeEarnedAnimation(name, drawableId);
                unlockBadge(layout, image, lock, drawableId);
                layout.setTag("EARNED");
            }
        });
    }

    private void startLockPulse(ImageView lock) {
        if (lock.getVisibility() != View.VISIBLE) return;
        
        // avoid restarting if already animating
        if (lock.getAnimation() != null || (lock.getTag() != null && "pulsing".equals(lock.getTag()))) return;
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(lock, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(lock, "scaleY", 1f, 1.2f, 1f);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);
        
        scaleX.start();
        scaleY.start();
        lock.setTag("pulsing");
    }

    private void checkBadgeStatus(View b1Layout, ImageView b1Image, ImageView b1Lock,
                                  View b2Layout, ImageView b2Image, ImageView b2Lock,
                                  View b3Layout, ImageView b3Image, ImageView b3Lock,
                                  TextView streakText) {
        
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GuideStats stats = snapshot.getValue(GuideStats.class);
                
                if (stats != null) {
                    streakText.setText("Current Technique Streak: " + stats.streakDays + " Days 🔥");

                    // badge 1 tag setter
                    if (stats.totalSessions >= badge1Threshold) {
                        String badgeId = "badge_10_sessions";
                        boolean hasBadge = stats.hasBadge(badgeId);
                        
                        if (hasBadge) {
                            unlockBadge(b1Layout, b1Image, b1Lock, R.drawable.badge_ten_sessions);
                            b1Layout.setTag("EARNED");
                        } else {
                            startLockPulse(b1Lock);
                            b1Layout.setTag("READY");
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // check for badge 3
        checkRescueUsage(b3Layout, b3Image, b3Lock, null);
    }

    private void checkRescueUsage(View layout, ImageView image, ImageView lock, GuideStats stats) {

        logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                        } catch (ParseException e) { }
                    }
                }

                if (countLast30Days < badge3Threshold) {
                    // check stats for account date and badge status
                    statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot statsSnapshot) {
                            GuideStats currentStats = statsSnapshot.getValue(GuideStats.class);
                            if (currentStats == null) currentStats = new GuideStats(0, 0, "", "");
                            
                            boolean accountOldEnough = false;
                            if (currentStats.accountCreationDate != null && !currentStats.accountCreationDate.isEmpty()) {
                                try {
                                    SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    Date creationDate = dateSdf.parse(currentStats.accountCreationDate);
                                    if (creationDate != null) {
                                        long diff = new Date().getTime() - creationDate.getTime();
                                        long daysSince = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                                        if (daysSince >= 30) {
                                            accountOldEnough = true;
                                        }
                                    }
                                } catch (ParseException e) { }
                            }
                            
                            if (accountOldEnough) {
                                String badgeId = "badge_well_controlled";
                                boolean hasBadge = currentStats.hasBadge(badgeId);
                                
                                if (hasBadge) {
                                    unlockBadge(layout, image, lock, R.drawable.badge_low_rescue_month);
                                    layout.setTag("EARNED");
                                } else {
                                    startLockPulse(lock);
                                    layout.setTag("READY");
                                }
                            }
                            
                            revealContent();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            revealContent();
                        }
                    });
                } else {
                    revealContent();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { 
                revealContent();
            }
        });
    }
    
    private void revealContent() {
        if (loadingSpinner != null) loadingSpinner.setVisibility(View.GONE);
        if (badgesScrollView != null) badgesScrollView.setVisibility(View.VISIBLE);
        if (textCurrentStreak != null) textCurrentStreak.setVisibility(View.VISIBLE);
    }

    private void unlockBadge(View layout, ImageView image, ImageView lock, int drawableId) {
        layout.setAlpha(1.0f);
        image.clearColorFilter(); 
        image.setImageResource(drawableId);
        image.setBackgroundColor(0); 
        lock.setVisibility(View.GONE);
        lock.clearAnimation(); // stop pulse if running
    }

    private void showBadgeEarnedAnimation(String badgeName, int drawableId) {
        if (overlayLayout == null) return;

        // play our super cool custom sound
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), R.raw.badge_unlock_sfx);
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(1.0f, 1.0f); // set volume to max
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        overlayBadgeImage.setImageResource(drawableId);
        overlayBadgeText.setText("Badge Earned:\n" + badgeName);
        
        // initialize scale to 0 so it starts invisible, we want the badge to grow into frame
        overlayBadgeImage.setScaleX(0f);
        overlayBadgeImage.setScaleY(0f);
        
        overlayLayout.setVisibility(View.VISIBLE);
        overlayLayout.setAlpha(0f);

        overlayLayout.animate().alpha(1f).setDuration(500).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(overlayBadgeImage, "scaleX", 0f, 1.8f, 1f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(overlayBadgeImage, "scaleY", 0f, 1.8f, 1f);
                ObjectAnimator rotate = ObjectAnimator.ofFloat(overlayBadgeImage, "rotation", 0f, 360f);
                scaleX.setDuration(2000); scaleY.setDuration(2000); rotate.setDuration(2000);
                scaleX.start(); scaleY.start(); rotate.start();

                Party party = new PartyFactory(new Emitter(100L, TimeUnit.MILLISECONDS).max(100))
                        .spread(360)
                        .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                        .setSpeedBetween(0f, 30f)
                        .position(0.5, 0.3)
                        .build();
                konfettiViewBadge.start(party);

                overlayLayout.postDelayed(() -> {
                    overlayLayout.animate().alpha(0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            overlayLayout.setVisibility(View.GONE);
                        }
                    }).start();
                }, 5000);
                
                // update firebase state to 'earned' so we don't show it again
                updateBadgeEarnedState(badgeName);
            }
        }).start();
    }
    
    private void updateBadgeEarnedState(String badgeName) {
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GuideStats stats = snapshot.getValue(GuideStats.class);
                if (stats != null) {
                    String id = badgeName.equals("Beginner Breather") ? "badge_10_sessions" : "badge_well_controlled";
                    stats.earnBadge(id);
                    statsRef.setValue(stats);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}