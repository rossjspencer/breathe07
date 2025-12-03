package com.example.smartair.r3;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.smartair.HomeFragment;
import com.example.smartair.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class InhalerFinishFragment extends Fragment {

    private static final String TEMP_USER_ID = "testUserId";
    private String currentUserId = TEMP_USER_ID;
    private static final String BADGE_CHANNEL_ID = "badge_alerts";
    
    private DatabaseReference statsRef;
    private DatabaseReference settingsRef;
    private boolean wasSuccessful = false;
    private int badge1Threshold = 10; // default

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_finish, container, false);

        createNotificationChannel();

        if (getArguments() != null) {
            wasSuccessful = getArguments().getBoolean("success", false);
            String argId = getArguments().getString("CHILD_ID");
            if (argId != null && !argId.isEmpty()) {
                currentUserId = argId;
            }
        }

        // prepare confetti animation
        KonfettiView konfettiView = view.findViewById(R.id.konfettiView);
        Button homeButton = view.findViewById(R.id.finishHomeButton);
        TextView statsText = view.findViewById(R.id.statsText);

        Party party = new PartyFactory(new Emitter(100L, TimeUnit.MILLISECONDS).max(100))
                .spread(360)
                .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                .setSpeedBetween(0f, 30f)
                .position(0.5, 0.3)
                .build();
        konfettiView.start(party);

        statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(currentUserId);
        settingsRef = FirebaseDatabase.getInstance().getReference("badge_settings").child(currentUserId);

        // load settings first, then update stats since badge 1 might be earned
        loadSettingsAndUpdateStats(statsText);

        homeButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }
    
    private void loadSettingsAndUpdateStats(TextView statsText) {
        settingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer b1 = snapshot.child("badge1_threshold").getValue(Integer.class);
                if (b1 != null) {
                    badge1Threshold = b1;
                }
                updateStats(statsText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateStats(statsText); // proceed with default
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

    // used here because we want to send the badge 1 notification as soon as it is earned
    // that happens on the inhaler finish page, and thus the method is here
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

    private void updateStats(TextView statsText) {
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GuideStats stats = snapshot.getValue(GuideStats.class);
                if (stats == null) {
                    stats = new GuideStats(0, 0, "", "");
                }

                // only update counters if session was successful
                if (wasSuccessful) {
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    
                    boolean sessionCountedToday = today.equals(stats.lastSessionDate);
                    
                    if (!sessionCountedToday) {
                        stats.totalSessions++;

                        // check streak
                        if (isConsecutiveDay(stats.lastSessionDate, today)) {
                            stats.streakDays++;
                        } else if (!today.equals(stats.lastSessionDate)) {
                            stats.streakDays = 1;
                        }
                        
                        stats.lastSessionDate = today;
                        statsRef.setValue(stats);
                    } else {
                        // increment total sessions even if same day
                        stats.totalSessions++;
                        statsRef.setValue(stats);
                    }

                    // check badge one sessions
                    if (stats.totalSessions >= badge1Threshold && !stats.hasBadge("badge_10_sessions")) {
                        sendBadgeNotification("Detail-Oriented");
                    }
                }

                // streak based on consecutive positive technique days
                if (getContext() != null) {
                    String text = "Total Sessions: " + stats.totalSessions + "\n" +
                            "Day Streak: " + stats.streakDays + " 🔥";
                    statsText.setText(text);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // handle error
            }
        });
    }

    // lil helper
    private boolean isConsecutiveDay(String lastDateStr, String todayStr) {
        if (lastDateStr == null || lastDateStr.isEmpty()) return false;
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date lastDate = sdf.parse(lastDateStr);
            Date today = sdf.parse(todayStr);
            
            if (lastDate == null || today == null) return false;

            long diff = today.getTime() - lastDate.getTime();
            long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
            
            return days == 1;
        } catch (Exception e) {
            return false;
        }
    }
}