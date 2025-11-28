package com.b07.asthmaid.r3;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
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

import com.b07.asthmaid.R;
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
    private static final String BADGE_CHANNEL_ID = "badge_alerts";
    
    private DatabaseReference statsRef;
    private boolean wasSuccessful = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_finish, container, false);

        createNotificationChannel();

        if (getArguments() != null) {
            wasSuccessful = getArguments().getBoolean("success", false);
        }

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

        statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(TEMP_USER_ID);

        updateStats(statsText);

        homeButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
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

    private void updateStats(TextView statsText) {
        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                GuideStats stats = snapshot.getValue(GuideStats.class);
                if (stats == null) {
                    stats = new GuideStats(0, 0, "");
                }

                if (wasSuccessful) {
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    
                    boolean sessionCountedToday = today.equals(stats.lastSessionDate);
                    
                    if (!sessionCountedToday) {
                        stats.totalSessions++;

                        if (isConsecutiveDay(stats.lastSessionDate, today)) {
                            stats.streakDays++;
                        } else if (!today.equals(stats.lastSessionDate)) {
                            stats.streakDays = 1;
                        }
                        
                        stats.lastSessionDate = today;
                        statsRef.setValue(stats);
                    } else {
                        stats.totalSessions++;
                        statsRef.setValue(stats);
                    }

                    if (stats.totalSessions >= 10 && !stats.hasBadge("badge_10_sessions")) {
                        sendBadgeNotification("Diaphragm Decathlon");
                    }
                }
                
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