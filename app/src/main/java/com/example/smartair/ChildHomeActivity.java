package com.example.smartair;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import android.content.Intent;
import com.example.smartair.R4.pef.PefEntryActivity;
import com.example.smartair.R4.triage.TriageActivity;
import com.example.smartair.r3.BadgesActivity;
import com.example.smartair.r3.ControllerLogEntry;
import com.example.smartair.r3.MedicineLogActivity;
import com.example.smartair.r3.MedicineLogFragment;
import com.example.smartair.r3.InhalerGuideActivity;
import com.example.smartair.r3.GuideStats;
import com.example.smartair.r3.RescueLogEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChildHomeActivity extends AppCompatActivity {

    private TextView tvCurrentZone, tvZoneDescription;
    private DatabaseReference mDatabase;
    private String currentChildId;
    private String childName = "";
    private int personalBest = 400;
    
    private static final String BADGE_CHANNEL_ID = "badge_alerts";
    private static final String PREFS_BADGES = "badge_prefs";
    public static final String EXTRA_CHILD_ID = "CHILD_ID";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_home);
        
        // Logout Button Logic
        Button btnLogout = findViewById(R.id.btnLogoutChild);
        btnLogout.setOnClickListener(v -> {
            // If using Firebase Auth (unlikely for child, but possible for parent impersonating)
            FirebaseAuth.getInstance().signOut();
            
            // Go to MainActivity (Login/Register)
            Intent intent = new Intent(ChildHomeActivity.this, MainActivity.class);
            // Clear the back stack so back button doesn't return here or exit app unexpectedly
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        tvCurrentZone = findViewById(R.id.tvCurrentZone);
        tvZoneDescription = findViewById(R.id.tvZoneDescription);
        
        Button btnTakeDose = findViewById(R.id.btnTakeDose);
        Button btnLogPef = findViewById(R.id.btnLogPef);
        Button btnLogSymptoms = findViewById(R.id.btnLogSymptoms);
        Button btnViewRescueLog = findViewById(R.id.btnViewRescueLog);
        Button btnViewControllerLog = findViewById(R.id.btnViewControllerLog);
        Button btnViewHistory = findViewById(R.id.btnViewHistory);
        Button btnAwards = findViewById(R.id.btnAwards);

        if (getIntent().hasExtra(EXTRA_CHILD_ID)) {
            currentChildId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentChildId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        // Triage button (keep)
        Button triageButton = findViewById(R.id.triage_button);
        triageButton.setOnClickListener(v -> {
            if (currentChildId == null || currentChildId.isEmpty()) {
                Toast.makeText(this, "No child selected for triage", Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(ChildHomeActivity.this, TriageActivity.class);
            i.putExtra(EXTRA_CHILD_ID, currentChildId); // use the same key you read
            startActivity(i);
        });


        mDatabase = FirebaseDatabase.getInstance().getReference();


        if (currentChildId != null) {
            loadChildData();
            checkWellControlledBadge();
            checkControllerStreak();
            checkPendingBadges();
        } else {
            Toast.makeText(this, "Error: Not Logged In", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        // Wire up Take Dose button
        btnTakeDose.setOnClickListener(v -> {
            Intent intent = new Intent(ChildHomeActivity.this, InhalerGuideActivity.class);
            intent.putExtra("CHILD_ID", currentChildId);
            startActivity(intent);
        });

        // Launch R4 PEF screen instead of dialog
        btnLogPef.setOnClickListener(v -> {
            Intent i = new Intent(ChildHomeActivity.this, PefEntryActivity.class);
            i.putExtra("childId", currentChildId);
            startActivity(i);
        });

        // Wire up log symptoms button
        btnLogSymptoms.setOnClickListener(v -> {
            Intent intent = new Intent(ChildHomeActivity.this, DailyLogActivity.class);
            intent.putExtra("CHILD_ID", currentChildId);
            intent.putExtra("LOGGED_BY_ROLE", "Child");
            intent.putExtra("CHILD_NAME", childName);
            startActivity(intent);
        });
        
        btnViewRescueLog.setOnClickListener(v -> {
            Intent intent = new Intent(ChildHomeActivity.this, MedicineLogActivity.class);
            intent.putExtra("CHILD_ID", currentChildId);
            intent.putExtra(MedicineLogFragment.ARG_TYPE, "RESCUE");
            startActivity(intent);
        });
        
        btnViewControllerLog.setOnClickListener(v -> {
            Intent intent = new Intent(ChildHomeActivity.this, MedicineLogActivity.class);
            intent.putExtra("CHILD_ID", currentChildId);
            intent.putExtra(MedicineLogFragment.ARG_TYPE, "CONTROLLER");
            startActivity(intent);
        });

        // Wire up history button
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(ChildHomeActivity.this, HistoryActivity.class);
            intent.putExtra("CHILD_ID", currentChildId);
            intent.putExtra("CHILD_NAME", childName);
            startActivity(intent);
        });
        
        // Wire up awards button
        btnAwards.setOnClickListener(v -> {
            Intent intent = new Intent(ChildHomeActivity.this, BadgesActivity.class);
            intent.putExtra("CHILD_ID", currentChildId);
            startActivity(intent);
        });
    }
    
    private void checkPendingBadges() {
        DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(currentChildId);
        statsRef.child("pendingNotifications").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot badgeSnap : snapshot.getChildren()) {
                    String badgeId = badgeSnap.getKey();
                    if (badgeId != null) {
                        String badgeName = getBadgeName(badgeId);
                        sendBadgeNotification(badgeName);
                        // Clear pending
                        statsRef.child("pendingNotifications").child(badgeId).removeValue();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private String getBadgeName(String badgeId) {
        switch (badgeId) {
            case "badge_technique_5": return "High Five";
            case "badge_technique_10": return "Technique Titan";
            case "badge_technique_30": return "Perfect Month";
            case "badge_streak_3": return "Streak Starter";
            case "badge_streak_7": return "Week Warrior";
            case "badge_streak_30": return "Commitment King";
            case "badge_well_controlled": return "Well Controlled";
            case "badge_perfect_week": return "Perfect Adherence";
            default: return "New Badge";
        }
    }

    private void loadChildData() {
        mDatabase.child("users").child(currentChildId).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.hasChild("firstName")) {
                        childName = snapshot.child("firstName").getValue(String.class);
                    }
                    Integer pb = snapshot.child("personalBest").getValue(Integer.class);
                    if (pb != null && pb > 0) personalBest = pb;

                    Integer score = snapshot.child("asthmaScore").getValue(Integer.class);
                    updateZoneUI(score != null ? score : 100);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateZoneUI(int score) {
        tvCurrentZone.setText(score + "%");
        if (score >= 80) {
            tvCurrentZone.setTextColor(Color.parseColor("#4CAF50"));
            tvZoneDescription.setText("Green Zone (All Good)");
            tvZoneDescription.setTextColor(Color.parseColor("#4CAF50"));
        } else if (score >= 50) {
            tvCurrentZone.setTextColor(Color.parseColor("#FFC107"));
            tvZoneDescription.setText("Yellow Zone (Caution)");
            tvZoneDescription.setTextColor(Color.parseColor("#FFC107"));
        } else {
            tvCurrentZone.setTextColor(Color.parseColor("#F44336"));
            tvZoneDescription.setText("Red Zone (Danger)");
            tvZoneDescription.setTextColor(Color.parseColor("#F44336"));
        }
    }
    
    private void checkWellControlledBadge() {
        DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(currentChildId);
        // Using medicine_logs/rescue
        DatabaseReference logsRef = FirebaseDatabase.getInstance("https://smartair-a6669-default-rtdb.firebaseio.com")
                .getReference("medicine_logs").child("rescue").child(currentChildId);
        DatabaseReference settingsRef = FirebaseDatabase.getInstance().getReference("badge_settings").child(currentChildId);
        
        settingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int badge3Threshold = 4; // Default
                Integer b3 = snapshot.child("badge3_threshold").getValue(Integer.class);
                if (b3 != null) badge3Threshold = b3;
                
                int finalThreshold = badge3Threshold;
                logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot logsSnap) {
                        Set<String> uniqueDays = new HashSet<>();
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DAY_OF_YEAR, -30);
                        Date thirtyDaysAgo = cal.getTime();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                        SimpleDateFormat daySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                        for (DataSnapshot child : logsSnap.getChildren()) {
                            RescueLogEntry entry = child.getValue(RescueLogEntry.class);
                            if (entry != null && entry.timestamp != null) {
                                try {
                                    Date entryDate = sdf.parse(entry.timestamp);
                                    if (entryDate != null && entryDate.after(thirtyDaysAgo)) {
                                        uniqueDays.add(daySdf.format(entryDate));
                                    }
                                } catch (ParseException e) { }
                            }
                        }
                        
                        if (uniqueDays.size() <= finalThreshold) {
                            statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot statsSnapshot) {
                                    GuideStats stats = statsSnapshot.getValue(GuideStats.class);
                                    if (stats == null) stats = new GuideStats(0, 0, "", "");
                                    
                                    boolean accountOldEnough = false;
                                    if (stats.accountCreationDate != null && !stats.accountCreationDate.isEmpty()) {
                                        try {
                                            SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                            Date creationDate = dateSdf.parse(stats.accountCreationDate);
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
                                        if (!stats.hasBadge("badge_well_controlled")) {
                                            // Check if already notified locally
                                            SharedPreferences prefs = getSharedPreferences(PREFS_BADGES, MODE_PRIVATE);
                                            boolean notified = prefs.getBoolean("notified_badge_well_controlled", false);
                                            
                                            if (!notified) {
                                                sendBadgeNotification("Well Controlled");
                                                prefs.edit().putBoolean("notified_badge_well_controlled", true).apply();
                                            }
                                        }
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e) {}
                            });
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }
    
    private void checkControllerStreak() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentChildId);
        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("medicine_logs").child("controller").child(currentChildId);
        DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(currentChildId);

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
                
                logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot logsSnap) {
                        if (calculateStreak(schedule, logsSnap)) {
                            statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot statsSnap) {
                                    GuideStats stats = statsSnap.getValue(GuideStats.class);
                                    if (stats == null) stats = new GuideStats(0, 0, "", "");
                                    
                                    if (!stats.hasBadge("badge_perfect_week")) {
                                        // Check if already notified locally
                                        SharedPreferences prefs = getSharedPreferences(PREFS_BADGES, MODE_PRIVATE);
                                        boolean notified = prefs.getBoolean("notified_badge_perfect_week", false);
                                        
                                        if (!notified) {
                                            sendBadgeNotification("Consistent Controller");
                                            prefs.edit().putBoolean("notified_badge_perfect_week", true).apply();
                                        }
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e) {}
                            });
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private boolean calculateStreak(Map<String, Integer> schedule, DataSnapshot logsSnap) {
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
                        String key = sdf.format(date);
                        dailyCount.put(key, dailyCount.getOrDefault(key, 0) + entry.doseCount);
                    }
                } catch (Exception e) {}
            }
        }
        
        Calendar iter = (Calendar) cal.clone();
        iter.add(Calendar.DAY_OF_YEAR, -6); 
        
        for (int i = 0; i < 7; i++) {
            String dateKey = sdf.format(iter.getTime());
            int dayOfWeek = iter.get(Calendar.DAY_OF_WEEK);
            String dayStr = getDayShortCode(dayOfWeek);
            
            int planned = schedule.getOrDefault(dayStr, 0);
            int actual = dailyCount.getOrDefault(dateKey, 0);
            
            if (actual < planned) {
                return false;
            }
            iter.add(Calendar.DAY_OF_YEAR, 1);
        }
        return true;
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
    
    private void sendBadgeNotification(String badgeName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Badge Alerts";
            String description = "Notifications for earned badges";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(BADGE_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BADGE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Badge Unlocked!")
                .setContentText("You earned the " + badgeName + " badge! Come claim it in Awards.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(badgeName.hashCode(), builder.build());
        } catch (SecurityException e) {
            // ignore
        }
    }
}