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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
    private static final int MAX_RESCUE_USES = 4;

    private RelativeLayout overlayLayout;
    private ImageView overlayBadgeImage;
    private TextView overlayBadgeText;
    private KonfettiView konfettiViewBadge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_badges, container, false);

        createNotificationChannel();

        Button backButton = view.findViewById(R.id.badgesBackButton);
        TextView textCurrentStreak = view.findViewById(R.id.textCurrentStreak);
        
        LinearLayout badge1Layout = view.findViewById(R.id.badge1Layout);
        ImageView badge1Image = view.findViewById(R.id.badge1Image);
        ImageView badge1Lock = view.findViewById(R.id.badge1Lock);

        LinearLayout badge2Layout = view.findViewById(R.id.badge2Layout);
        ImageView badge2Image = view.findViewById(R.id.badge2Image);
        ImageView badge2Lock = view.findViewById(R.id.badge2Lock);

        LinearLayout badge3Layout = view.findViewById(R.id.badge3Layout);
        ImageView badge3Image = view.findViewById(R.id.badge3Image);
        ImageView badge3Lock = view.findViewById(R.id.badge3Lock);

        if (getActivity() != null) {
            overlayLayout = getActivity().findViewById(R.id.badgeOverlayLayout);
            overlayBadgeImage = getActivity().findViewById(R.id.mainOverlayBadgeImage);
            overlayBadgeText = getActivity().findViewById(R.id.mainOverlayBadgeText);
            konfettiViewBadge = getActivity().findViewById(R.id.mainKonfettiView);
        }

        statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(TEMP_USER_ID);
        logsRef = FirebaseDatabase.getInstance().getReference("medicine_logs").child("rescue").child(TEMP_USER_ID);

        // initial grayscale application
        applyGrayscale(badge1Image);
        applyGrayscale(badge2Image);
        applyGrayscale(badge3Image);

        // setup lock click listeners for animation
        setupBadgeClick(badge1Layout, badge1Image, badge1Lock, "Diaphragm Decathlon", R.drawable.badge_ten_sessions);
        setupBadgeClick(badge3Layout, badge3Image, badge3Lock, "Well Controlled", R.drawable.badge_low_rescue_month);

        checkBadgeStatus(badge1Layout, badge1Image, badge1Lock,
                badge2Layout, badge2Image, badge2Lock,
                badge3Layout, badge3Image, badge3Lock, textCurrentStreak);

        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        return view;
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

    private void sendBadgeNotification(String badgeName) {
        if (getContext() == null) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), BADGE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Badge Unlocked!")
                .setContentText("The " + badgeName + " badge is ready to be unlocked. Go to the awards screen to claim it!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        try {
            notificationManager.notify(badgeName.hashCode(), builder.build());
        } catch (SecurityException e) {
            // ignore
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
                showBadgeEarnedAnimation(name, drawableId);

                unlockBadge(layout, image, lock, drawableId);

                layout.setTag("EARNED");
            }
        });
    }

    private void startLockPulse(ImageView lock) {
        if (lock.getVisibility() != View.VISIBLE) return;
        
        // Avoid restarting if already animating
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

    private void checkBadgeStatus(LinearLayout b1Layout, ImageView b1Image, ImageView b1Lock,
                                  LinearLayout b2Layout, ImageView b2Image, ImageView b2Lock,
                                  LinearLayout b3Layout, ImageView b3Image, ImageView b3Lock,
                                  TextView streakText) {
        
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GuideStats stats = snapshot.getValue(GuideStats.class);
                
                if (stats != null) {
                    streakText.setText("Current Streak: " + stats.streakDays + " Days 🔥");

                    // Badge 1
                    if (stats.totalSessions >= 10) {
                        String badgeId = "badge_10_sessions";
                        boolean hasBadge = stats.hasBadge(badgeId);
                        
                        if (hasBadge) {
                            unlockBadge(b1Layout, b1Image, b1Lock, R.drawable.badge_ten_sessions);
                            b1Layout.setTag("EARNED");
                        } else {
                            startLockPulse(b1Lock);
                            b1Layout.setTag("READY");
                            sendBadgeNotification("Diaphragm Decathlon");
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        checkRescueUsage(b3Layout, b3Image, b3Lock);
    }

    private void checkRescueUsage(LinearLayout layout, ImageView image, ImageView lock) {
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

                if (countLast30Days < MAX_RESCUE_USES) {
                    statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot statsSnapshot) {
                            GuideStats stats = statsSnapshot.getValue(GuideStats.class);
                            if (stats == null) stats = new GuideStats(0, 0, "");
                            
                            String badgeId = "badge_well_controlled";
                            boolean hasBadge = stats.hasBadge(badgeId);
                            
                            if (hasBadge) {
                                unlockBadge(layout, image, lock, R.drawable.badge_low_rescue_month);
                                layout.setTag("EARNED");
                            } else {
                                startLockPulse(lock);
                                layout.setTag("READY");
                                sendBadgeNotification("Well Controlled");
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void unlockBadge(View layout, ImageView image, ImageView lock, int drawableId) {
        layout.setAlpha(1.0f);
        image.clearColorFilter(); 
        image.setImageResource(drawableId);
        image.setBackgroundColor(0); 
        lock.setVisibility(View.GONE);
        lock.clearAnimation();
    }

    private void showBadgeEarnedAnimation(String badgeName, int drawableId) {
        if (overlayLayout == null) return;

        // play sound
        try {
            
            MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), R.raw.badge_unlock_sfx);
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(1f, 1f);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        overlayBadgeImage.setImageResource(drawableId);
        overlayBadgeText.setText("Badge Earned:\n" + badgeName);
        
        // initialize scale to 0 so it starts invisible
        overlayBadgeImage.setScaleX(0f);
        overlayBadgeImage.setScaleY(0f);
        
        overlayLayout.setVisibility(View.VISIBLE);
        overlayLayout.setAlpha(0f);

        overlayLayout.animate().alpha(1f).setDuration(500).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(overlayBadgeImage, "scaleX", 0f, 1.8f, 1.2f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(overlayBadgeImage, "scaleY", 0f, 1.8f, 1.2f);
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
                }, 6000);
                
                // update firebase state to 'earned'
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
                    String id = badgeName.equals("Diaphragm Decathlon") ? "badge_10_sessions" : "badge_well_controlled";
                    stats.earnBadge(id);
                    statsRef.setValue(stats);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}