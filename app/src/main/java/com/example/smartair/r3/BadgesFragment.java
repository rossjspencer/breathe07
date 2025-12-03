package com.example.smartair.r3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.example.smartair.R;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class BadgesFragment extends Fragment {

    private static final String TEMP_USER_ID = "testUserId";
    private String currentUserId = TEMP_USER_ID;
    private static final String BADGE_CHANNEL_ID = "badge_alerts";
    
    private DatabaseReference statsRef;
    private DatabaseReference logsRef;
    private DatabaseReference controllerLogsRef;
    private DatabaseReference settingsRef;
    private DatabaseReference userRef;

    // defaults
    private int badge1Threshold = 10;
    private int badge3Threshold = 4;

    private RelativeLayout overlayLayout;
    private ImageView overlayBadgeImage;
    private TextView overlayBadgeText;
    private KonfettiView konfettiViewBadge;
    
    private TextView badge1Desc;
    private TextView badge2Desc;
    private TextView badge3Desc;
    private TextView textCurrentStreak;
    private TextView textControllerStreak;
    
    private ProgressBar loadingSpinner;
    private ScrollView badgesScrollView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_badges, container, false);

        if (getArguments() != null) {
            String argId = getArguments().getString("CHILD_ID");
            if (argId != null && !argId.isEmpty()) {
                currentUserId = argId;
            }
        }

        createNotificationChannel();

        textCurrentStreak = view.findViewById(R.id.textCurrentStreak);
        textCurrentStreak.setVisibility(View.INVISIBLE);
        
        textControllerStreak = view.findViewById(R.id.textControllerStreak);
        textControllerStreak.setVisibility(View.INVISIBLE);
        
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        badgesScrollView = view.findViewById(R.id.badgesScrollView);
        
        LinearLayout badge1Layout = view.findViewById(R.id.badge1Layout);
        FrameLayout badge1Frame = view.findViewById(R.id.badge1Frame); 
        ImageView badge1Image = view.findViewById(R.id.badge1Image);
        ImageView badge1Lock = view.findViewById(R.id.badge1Lock);
        badge1Desc = view.findViewById(R.id.badge1Desc);

        LinearLayout badge2Layout = view.findViewById(R.id.badge2Layout);
        FrameLayout badge2Frame = view.findViewById(R.id.badge2Frame); 
        ImageView badge2Image = view.findViewById(R.id.badge2Image);
        ImageView badge2Lock = view.findViewById(R.id.badge2Lock);
        try {
            badge2Desc = view.findViewById(R.id.badge2Desc);
        } catch (Exception e) {}

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

        statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(currentUserId);
        logsRef = FirebaseDatabase.getInstance().getReference("medicine_logs").child("rescue").child(currentUserId);
        controllerLogsRef = FirebaseDatabase.getInstance().getReference("medicine_logs").child("controller").child(currentUserId);
        settingsRef = FirebaseDatabase.getInstance().getReference("badge_settings").child(currentUserId);
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);

        // badges should be grayscale by default (until unlocked)
        applyGrayscale(badge1Image);
        applyGrayscale(badge2Image);
        applyGrayscale(badge3Image);

        setupBadgeClick(badge1Frame, badge1Image, badge1Lock, "Detail-Oriented", R.drawable.badge_ten_sessions);
        setupBadgeClick(badge2Frame, badge2Image, badge2Lock, "Consistent Controller", R.drawable.badge_perfect_week);
        setupBadgeClick(badge3Frame, badge3Image, badge3Lock, "Well Controlled", R.drawable.badge_low_rescue_month);

        loadSettingsAndCheckStatus(badge1Frame, badge1Image, badge1Lock,
                badge2Frame, badge2Image, badge2Lock,
                badge3Frame, badge3Image, badge3Lock, textCurrentStreak, textControllerStreak);

        return view;
    }
    
    // loads settings from firebase and then checks badge status
    private void loadSettingsAndCheckStatus(View b1Layout, ImageView b1Image, ImageView b1Lock,
                                            View b2Layout, ImageView b2Image, ImageView b2Lock,
                                            View b3Layout, ImageView b3Image, ImageView b3Lock,
                                            TextView streakText, TextView controllerStreakText) {
        settingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer b1 = snapshot.child("badge1_threshold").getValue(Integer.class);
                Integer b3 = snapshot.child("badge3_threshold").getValue(Integer.class);
                
                if (b1 != null) badge1Threshold = b1;
                if (b3 != null) badge3Threshold = b3;

                // set badge descriptions based on parent settings
                if (badge1Desc != null) {
                    badge1Desc.setText("Complete " + badge1Threshold + " high-quality inhaler doses");
                }
                if (badge2Desc != null) {
                    badge2Desc.setText("Follow controller schedule for 7 consecutive days");
                }
                if (badge3Desc != null) {
                    badge3Desc.setText("Use your Rescue inhaler on " + badge3Threshold + " or fewer days in 30 days");
                }

                checkBadgeStatus(b1Layout, b1Image, b1Lock, b2Layout, b2Image, b2Lock, b3Layout, b3Image, b3Lock, streakText, controllerStreakText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                checkBadgeStatus(b1Layout, b1Image, b1Lock, b2Layout, b2Image, b2Lock, b3Layout, b3Image, b3Lock, streakText, controllerStreakText);
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

    private void sendBadgeNotification(String badgeName) {
        if (getContext() == null) return;
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), BADGE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Badge Unlocked!")
                .setContentText("You earned the " + badgeName + " badge! Come claim it in Awards!")
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

    // applies a grayscale filter to the badge image
    private void applyGrayscale(ImageView imageView) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        imageView.setColorFilter(filter);
    }

    // sets up the click listener for the badge to play animation and unlock
    private void setupBadgeClick(View layout, ImageView image, ImageView lock, String name, int drawableId) {
        if (layout == null) return;
        layout.setClickable(true);
        
        layout.setOnClickListener(v -> {
            Object tag = layout.getTag();
            if ("READY".equals(tag)) {
                showBadgeEarnedAnimation(name, drawableId);
                unlockBadge(layout, image, lock, drawableId);
                layout.setTag("EARNED");
            }
        });
    }

    // pulses the lock icon if the badge is ready to be unlocked
    private void startLockPulse(ImageView lock) {
        if (lock.getVisibility() != View.VISIBLE) return;
        
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
                                  TextView streakText, TextView controllerStreakText) {
        
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GuideStats stats = snapshot.getValue(GuideStats.class);
                
                if (stats != null) {
                    String dayLabel = stats.streakDays == 1 ? " Day" : " Days";
                    streakText.setText("Technique Streak: " + stats.streakDays + dayLabel + " 🔥");
                    
                    String cDayLabel = stats.controllerStreakDays == 1 ? " Day" : " Days";
                    controllerStreakText.setText("Controller Streak: " + stats.controllerStreakDays + cDayLabel + " ⭐");

                    // badge 1
                    if (stats.totalSessions >= badge1Threshold) {
                        String badgeId = "badge_10_sessions";
                        boolean hasBadge = stats.hasBadge(badgeId);
                        
                        if (hasBadge) {
                            unlockBadge(b1Layout, b1Image, b1Lock, R.drawable.badge_ten_sessions);
                            b1Layout.setTag("EARNED");
                        } else {
                            startLockPulse(b1Lock);
                            b1Layout.setTag("READY");
                            // sendBadgeNotification("Detail-Oriented");
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        //other two badge checks are a bit more complex, so they get offloaded
        // check for badge 2 and controller streak
        checkControllerStreak(b2Layout, b2Image, b2Lock, controllerStreakText);

        // check for badge 3
        checkRescueUsage(b3Layout, b3Image, b3Lock, null);
    }

    private void checkControllerStreak(View layout, ImageView image, ImageView lock, TextView controllerStreakText) {
        userRef.child("plannedSchedule").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot scheduleSnap) {
                Map<String, Integer> schedule = new HashMap<>();
                for (DataSnapshot d : scheduleSnap.getChildren()) {
                    Integer val = d.getValue(Integer.class);
                    if (d.getKey() != null && val != null) {
                        schedule.put(d.getKey(), val);
                    }
                }
                
                controllerLogsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot logsSnap) {
                        checkStreakLogic(layout, image, lock, schedule, logsSnap, controllerStreakText);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    // calculate the controller adherence streak based on the schedule
    private void checkStreakLogic(View layout, ImageView image, ImageView lock, Map<String, Integer> schedule, DataSnapshot logsSnap, TextView controllerStreakText) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        Map<String, Integer> dailyCount = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        
        for (DataSnapshot d : logsSnap.getChildren()) {
            ControllerLogEntry entry = d.getValue(ControllerLogEntry.class);
            if (entry != null && entry.timestamp != null) {
                try {
                    Date date = fullSdf.parse(entry.timestamp);
                    if (date != null) {
                        dailyCount.put(sdf.format(date), dailyCount.getOrDefault(sdf.format(date), 0) + entry.doseCount);
                    }
                } catch (Exception e) {}
            }
        }

        // calculate current streak
        int currentStreak = 0;

        int backStreak = 0;
        Calendar backIter = (Calendar) cal.clone();
        backIter.add(Calendar.DAY_OF_YEAR, -1);

        // flag indicating if the streak has "started" yet
        boolean streakStarted = false;

        // limit lookback to 365 days
        for (int i = 0; i < 365; i++) {

            String key = sdf.format(backIter.getTime());
            int dw = backIter.get(Calendar.DAY_OF_WEEK);
            String ds = getDayShortCode(dw);

            int planned = schedule.getOrDefault(ds, 0);
            int actual = dailyCount.getOrDefault(key, 0);

            boolean adherent = actual >= planned;

            if (!streakStarted) {
                // first day must:
                // 1. be a controller day (planned > 0)
                // 2. be adherent
                if (planned > 0 && adherent) {
                    streakStarted = true;
                    backStreak++;
                } else {
                    // streak cannot start
                    break;
                }
            } else {
                // after first day, normal adherence rules
                if (adherent) {
                    backStreak++;
                } else {
                    break;
                }
            }

            backIter.add(Calendar.DAY_OF_YEAR, -1);
        }

        // check today
        String tKey = sdf.format(cal.getTime());
        int tDw = cal.get(Calendar.DAY_OF_WEEK);
        String tDs = getDayShortCode(tDw);
        int tPlanned = schedule.getOrDefault(tDs, 0);
        int tActual  = dailyCount.getOrDefault(tKey, 0);

        boolean todayAdherent = tActual >= tPlanned;

        // today extends the streak ONLY if the streak behind it was valid
        if (streakStarted && todayAdherent) {
            currentStreak = backStreak + 1;
        } else if (streakStarted) {
            // streak valid but today not adherent
            currentStreak = backStreak;
        } else {
            // no valid streak start
            currentStreak = 0;
        }
        
        // update text
        if (controllerStreakText != null) {
            String label = currentStreak == 1 ? " Day" : " Days";
            controllerStreakText.setText("Controller Streak: " + currentStreak + label + " ⭐");
            controllerStreakText.setVisibility(View.VISIBLE);
        }
        
        // update stats
        int finalStreak = currentStreak;
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GuideStats stats = snapshot.getValue(GuideStats.class);
                if (stats == null) stats = new GuideStats(0, 0, "", "");
                
                if (stats.controllerStreakDays != finalStreak) {
                    stats.controllerStreakDays = finalStreak;
                    statsRef.setValue(stats);
                }
                
                // check badge 2 (7 days)
                boolean hasBadge = stats.hasBadge("badge_perfect_week");
                if (finalStreak >= 7) {
                    if (hasBadge) {
                        unlockBadge(layout, image, lock, R.drawable.badge_perfect_week);
                        layout.setTag("EARNED");
                    } else {
                        startLockPulse(lock);
                        layout.setTag("READY");
                        sendBadgeNotification("Consistent Controller");
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    // checks if the user has used their rescue inhaler too many times
    private void checkRescueUsage(View layout, ImageView image, ImageView lock, GuideStats stats) {

        logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> daysWithRescue = new HashSet<>();
                
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -30);
                Date thirtyDaysAgo = cal.getTime();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat daySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                for (DataSnapshot child : snapshot.getChildren()) {
                    RescueLogEntry entry = child.getValue(RescueLogEntry.class);
                    if (entry != null && entry.timestamp != null) {
                        try {
                            Date entryDate = sdf.parse(entry.timestamp);
                            if (entryDate != null && entryDate.after(thirtyDaysAgo)) {
                                daysWithRescue.add(daySdf.format(entryDate));
                            }
                        } catch (ParseException e) { }
                    }
                }

                if (daysWithRescue.size() <= badge3Threshold) {
                    // check stats for account date and badge status
                    statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot statsSnapshot) {
                            GuideStats currentStats = statsSnapshot.getValue(GuideStats.class);
                            if (currentStats == null) currentStats = new GuideStats(0, 0, "", "");

                            // account must be at least 30 days old for the well controlled badge
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
                                    sendBadgeNotification("Well Controlled");
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
        if (textControllerStreak != null) textControllerStreak.setVisibility(View.VISIBLE);
    }

    // unlocks the badge by removing the lock and setting the image
    private void unlockBadge(View layout, ImageView image, ImageView lock, int drawableId) {
        if (layout == null || image == null || lock == null) return;
        layout.setAlpha(1.0f);
        image.clearColorFilter(); 
        image.setImageResource(drawableId);
        image.setBackgroundColor(0); 
        lock.setVisibility(View.GONE);
        lock.clearAnimation(); // stop pulse if running
    }

    // shows the full screen animation when a badge is earned
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

    // keeps badges from being earned multiple times
    private void updateBadgeEarnedState(String badgeName) {
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GuideStats stats = snapshot.getValue(GuideStats.class);
                if (stats != null) {
                    String id;
                    if (badgeName.equals("Detail-Oriented")) id = "badge_10_sessions";
                    else if (badgeName.equals("Consistent Controller")) id = "badge_perfect_week";
                    else id = "badge_well_controlled";
                    
                    stats.earnBadge(id);
                    statsRef.setValue(stats);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private String getDayShortCode(int day) {
        switch (day) {
            case Calendar.SUNDAY: return "Sun";
            case Calendar.MONDAY: return "Mon";
            case Calendar.TUESDAY: return "Tue";
            case Calendar.WEDNESDAY: return "Wed";
            case Calendar.THURSDAY: return "Thu";
            case Calendar.FRIDAY: return "Fri";
            case Calendar.SATURDAY: return "Sat";
            default: return "Mon";
        }
    }
}