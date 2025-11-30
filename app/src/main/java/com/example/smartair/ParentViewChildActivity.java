package com.example.smartair;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.example.smartair.R4.pef.ParentSetPbActivity;
import com.google.firebase.database.ChildEventListener;

public class ParentViewChildActivity extends AppCompatActivity {

    private String childId;
    private DatabaseReference mDatabase;

    private TextView tvTitle, tvSharingTag, tvZoneValue, tvZoneSubtitle, tvLastRescue, tvWeeklyCount, tvAdherenceValue, tvAdherencePlan;
    private View cardZone, cardRescue, cardTrend, cardAdherence;
    private TrendChartView trendChartView;
    private Button btnRange7, btnRange30, btnGenerateReport, btnQuickRescue, btnQuickController, btnViewReport, btnExpandTrend;
    private Map<String, Boolean> sharingSettings = new HashMap<>();

    private final List<RescueLog> rescueLogs = new ArrayList<>();
    private final List<ControllerLog> controllerLogs = new ArrayList<>();
    private User childProfile;
    private int selectedRangeDays = 7;
    private AlertHelper alertHelper;

    private Button btnSetPb;

    /* Triage Banner
    private TextView triageBanner;
    private ChildEventListener triageListener;
    private static final String PREFS_TRIAGE_SEEN = "triage_seen_prefs";
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_view_child);

        childId = getIntent().getStringExtra("CHILD_ID");
        if (childId == null) {
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        alertHelper = new AlertHelper(this);

        bindViews();
        wireActions();
        loadProfile();
        loadSharing();
        listenForLogs();
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvChildTitle);
        tvSharingTag = findViewById(R.id.tvSharingTag);
        tvZoneValue = findViewById(R.id.tvZoneValue);
        tvZoneSubtitle = findViewById(R.id.tvZoneSubtitle);
        tvLastRescue = findViewById(R.id.tvLastRescue);
        tvWeeklyCount = findViewById(R.id.tvWeeklyCount);
        tvAdherenceValue = findViewById(R.id.tvAdherenceValue);
        tvAdherencePlan = findViewById(R.id.tvAdherencePlan);
        trendChartView = findViewById(R.id.trendChart);
        btnRange7 = findViewById(R.id.btnRange7);
        btnRange30 = findViewById(R.id.btnRange30);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        btnQuickRescue = findViewById(R.id.btnQuickRescue);
        btnQuickController = findViewById(R.id.btnQuickController);
        btnViewReport = findViewById(R.id.btnViewReport);
        btnExpandTrend = findViewById(R.id.btnExpandTrend);
        btnSetPb = findViewById(R.id.btn_set_pb);
        cardZone = findViewById(R.id.cardZone);
        cardRescue = findViewById(R.id.cardRescue);
        cardTrend = findViewById(R.id.cardTrend);
        cardAdherence = findViewById(R.id.cardAdherence);
        //triageBanner = findViewById(R.id.triageBanner);
    }

    private void wireActions() {
        btnRange7.setOnClickListener(v -> {
            selectedRangeDays = 7;
            refreshTrend();
        });
        btnRange30.setOnClickListener(v -> {
            selectedRangeDays = 30;
            refreshTrend();
        });
        btnGenerateReport.setOnClickListener(v -> {
            ProviderReportActivity.launch(this, childId, selectedRangeDays);
        });
        btnQuickRescue.setOnClickListener(v -> showRescueLogDialog());
        btnQuickController.setOnClickListener(v -> showControllerLogDialog());
        btnViewReport.setOnClickListener(v -> openExistingReport());
        btnExpandTrend.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrendDetailActivity.class);
            intent.putExtra(TrendDetailActivity.EXTRA_CHILD_ID, childId);
            startActivity(intent);
        });

        cardZone.setOnClickListener(v -> {
            // Open the child's own dashboard for deeper detail
            Intent intent = new Intent(this, ChildHomeActivity.class);
            intent.putExtra("CHILD_ID", childId);
            startActivity(intent);
        });

        cardRescue.setOnClickListener(v -> showRescueLogDialog());
        cardAdherence.setOnClickListener(v -> {
            // Jump to manage child so parent can adjust controller plan
            Intent intent = new Intent(this, ManageChildActivity.class);
            intent.putExtra("CHILD_ID", childId);
            startActivity(intent);
        });
        cardTrend.setOnClickListener(v -> {
            selectedRangeDays = selectedRangeDays == 7 ? 30 : 7;
            refreshTrend();
        });

        btnSetPb.setOnClickListener(v -> {
            Intent i = new Intent(this, ParentSetPbActivity.class);
            i.putExtra(ParentSetPbActivity.EXTRA_CHILD_ID, childId); // pass the child being viewed
            startActivity(i);
        });

    }

    private void openExistingReport() {
        String path = ProviderReportActivity.getReportFilePath(this, childId);
        java.io.File f = new java.io.File(path);
        if (f.exists()) {
            Intent intent = new Intent(this, PdfPreviewActivity.class);
            intent.putExtra(PdfPreviewActivity.EXTRA_PATH, path);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No report file found. Generate one first.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfile() {
        mDatabase.child("users").child(childId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childProfile = snapshot.getValue(User.class);
                if (childProfile != null) {
                    childProfile.userId = childId;
                    String name = (childProfile.firstName != null ? childProfile.firstName : "Child") +
                            (childProfile.lastName != null ? " " + childProfile.lastName : "");
                    tvTitle.setText(name + " - Parent Dashboard");
                    updateZoneTile(childProfile.asthmaScore);
                    updateAdherenceTile();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadSharing() {
        mDatabase.child("users").child(childId).child("sharingSettings")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        sharingSettings.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Boolean val = child.getValue(Boolean.class);
                            sharingSettings.put(child.getKey(), val != null && val);
                        }
                        boolean isShared = sharingSettings.getOrDefault("isShared", false);
                        tvSharingTag.setVisibility(isShared ? View.VISIBLE : View.GONE);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void listenForLogs() {
        mDatabase.child("users").child(childId).child("rescueLogs")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        rescueLogs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            RescueLog log = child.getValue(RescueLog.class);
                            if (log != null) rescueLogs.add(log);
                        }
                        updateRescueTile();
                        refreshTrend();
                        checkAlerts();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        mDatabase.child("users").child(childId).child("controllerLogs")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        controllerLogs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ControllerLog log = child.getValue(ControllerLog.class);
                            if (log != null) controllerLogs.add(log);
                        }
                        updateAdherenceTile();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
    //TRIAGE BANNER
    /*
    private long getLastSeenTriageTs(String childId) {
        return getSharedPreferences(PREFS_TRIAGE_SEEN, MODE_PRIVATE)
                .getLong("lastSeenTs_" + childId, 0L);
    }

    private void setLastSeenTriageTs(String childId, long ts) {
        getSharedPreferences(PREFS_TRIAGE_SEEN, MODE_PRIVATE)
                .edit()
                .putLong("lastSeenTs_" + childId, ts)
                .apply();
    }

    private void showTriageBanner() {
        if (triageBanner.getVisibility() != View.VISIBLE) {
            triageBanner.setVisibility(View.VISIBLE);
            triageBanner.setOnClickListener(v -> {
                // Mark latest as seen, then hide
                mDatabase.child("users").child(childId).child("triageIncidents")
                        .orderByChild("timestamp").limitToLast(1)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                                long maxTs = 0L;
                                for (DataSnapshot ds : snapshot.getChildren()) {
                                    Long ts = ds.child("timestamp").getValue(Long.class);
                                    if (ts != null && ts > maxTs) maxTs = ts;
                                }
                                if (maxTs > 0L) setLastSeenTriageTs(childId, maxTs);
                                triageBanner.setVisibility(View.GONE);
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
            });
        }
    }

    private void startTriageListener() {
        if (triageListener != null) return;

        DatabaseReference triageRef = mDatabase.child("users")
                .child(childId)
                .child("triageIncidents");

        triageListener = new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snap, String prev) {
                Long ts = snap.child("timestamp").getValue(Long.class);
                if (ts == null) return;
                if (ts > getLastSeenTriageTs(childId)) {
                    showTriageBanner();
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };

        triageRef.addChildEventListener(triageListener);

        // Also check most recent on entry, in case it arrived while screen wasn’t active
        triageRef.orderByChild("timestamp").limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Long ts = ds.child("timestamp").getValue(Long.class);
                            if (ts != null && ts > getLastSeenTriageTs(childId)) {
                                showTriageBanner();
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void stopTriageListener() {
        if (triageListener != null) {
            mDatabase.child("users").child(childId).child("triageIncidents")
                    .removeEventListener(triageListener);
            triageListener = null;
        }
    }
    */

    private void updateZoneTile(int score) {
        tvZoneValue.setText(score + "%");
        if (score >= 80) {
            tvZoneValue.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            tvZoneSubtitle.setText("Green Zone");
        } else if (score >= 50) {
            tvZoneValue.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            tvZoneSubtitle.setText("Yellow Zone");
        } else {
            tvZoneValue.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvZoneSubtitle.setText("Red Zone");
        }
        checkAlerts();
    }

    private void updateRescueTile() {
        if (rescueLogs.isEmpty()) {
            tvLastRescue.setText("No rescue logged");
            tvWeeklyCount.setText("Weekly count: 0");
            return;
        }
        RescueLog latest = rescueLogs.get(0);
        for (RescueLog log : rescueLogs) {
            if (log.timestamp > latest.timestamp) latest = log;
        }
        DateFormat df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, Locale.getDefault());
        tvLastRescue.setText(df.format(new Date(latest.timestamp)));

        int weekly = countRescuesInDays(7);
        tvWeeklyCount.setText("Weekly count: " + weekly);
    }

    private void refreshTrend() {
        List<Integer> points = buildDailyCounts(selectedRangeDays);
        trendChartView.setData(points, selectedRangeDays);
        btnRange7.setEnabled(selectedRangeDays != 7);
        btnRange30.setEnabled(selectedRangeDays != 30);
    }

    private List<Integer> buildDailyCounts(int days) {
        long now = System.currentTimeMillis();
        long dayMs = 24L * 60 * 60 * 1000;
        List<Integer> counts = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            long start = now - (i + 1) * dayMs;
            long end = now - i * dayMs;
            int count = 0;
            for (RescueLog log : rescueLogs) {
                if (log.timestamp >= start && log.timestamp < end) count++;
            }
            counts.add(count);
        }
        return counts;
    }

    private int countRescuesInDays(int days) {
        long now = System.currentTimeMillis();
        long range = days * 24L * 60 * 60 * 1000;
        int count = 0;
        for (RescueLog log : rescueLogs) {
            if (now - log.timestamp <= range) count++;
        }
        return count;
    }

    private void updateAdherenceTile() {
        if (childProfile == null) return;
        int perDay = Math.max(1, childProfile.plannedControllerPerDay);
        int perWeek = Math.max(1, childProfile.plannedControllerDaysPerWeek);
        tvAdherencePlan.setText("Plan: " + perDay + "x daily, " + perWeek + " days/week");

        long now = System.currentTimeMillis();
        long thirtyDaysMs = 30L * 24 * 60 * 60 * 1000;
        Map<String, Integer> dailyCount = new HashMap<>();
        for (ControllerLog log : controllerLogs) {
            if (now - log.timestamp > thirtyDaysMs) continue;
            String dayKey = dayKey(log.timestamp);
            int current = dailyCount.containsKey(dayKey) ? dailyCount.get(dayKey) : 0;
            dailyCount.put(dayKey, current + log.doses);
        }

        int plannedDays = Math.max(1, (int) Math.round((perWeek / 7.0) * 30));
        int completedDays = 0;
        for (Integer doses : dailyCount.values()) {
            if (doses >= perDay) completedDays++;
        }
        double adherence = (completedDays / (double) plannedDays) * 100;
        if (adherence > 100) adherence = 100;
        tvAdherenceValue.setText(String.format(Locale.getDefault(), "%.0f%%", adherence));
    }

    private String dayKey(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int y = cal.get(Calendar.YEAR);
        int d = cal.get(Calendar.DAY_OF_YEAR);
        return y + "-" + d;
    }

    private void showRescueLogDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_log_rescue, null);
        EditText etDose = dialogView.findViewById(R.id.etDose);
        EditText etFeeling = dialogView.findViewById(R.id.etFeeling);

        new AlertDialog.Builder(this)
                .setTitle("Log Rescue Use")
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    int dose = parseInt(etDose.getText().toString(), 1);
                    boolean worseAfterDose = !TextUtils.isEmpty(etFeeling.getText()) &&
                            etFeeling.getText().toString().toLowerCase().contains("worse");
                    RescueLog log = new RescueLog(System.currentTimeMillis(), dose, worseAfterDose);
                    mDatabase.child("users").child(childId).child("rescueLogs").push().setValue(log);
                    mDatabase.child("users").child(childId).child("lastRescueTimestamp").setValue(log.timestamp);
                    Toast.makeText(this, "Rescue logged", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showControllerLogDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_log_controller, null);
        EditText etDose = dialogView.findViewById(R.id.etControllerDose);

        new AlertDialog.Builder(this)
                .setTitle("Log Controller Dose")
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    int dose = parseInt(etDose.getText().toString(), 1);
                    ControllerLog log = new ControllerLog(System.currentTimeMillis(), dose);
                    mDatabase.child("users").child(childId).child("controllerLogs").push().setValue(log);
                    Toast.makeText(this, "Controller logged", Toast.LENGTH_SHORT).show();
                    updateAdherenceTile();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void checkAlerts() {
        if (childProfile == null) return;

        // Red zone alert
        if (childProfile.asthmaScore < 50) {
            alertHelper.maybeNotify("redZone", "Red-zone day", "Child is currently in the Red Zone. Review action plan.");
        }

        // Rapid rescue repeats: >=3 within 3 hours
        int rapidCount = 0;
        long now = System.currentTimeMillis();
        for (int i = rescueLogs.size() - 1; i >= 0; i--) {
            RescueLog log = rescueLogs.get(i);
            if (now - log.timestamp <= 3 * 60 * 60 * 1000) rapidCount++;
        }
        if (rapidCount >= 3) {
            alertHelper.maybeNotify("rapidRescue", "Rapid rescue repeats", "3+ rescue uses in 3 hours.");
        }

        // Worse after dose flag
        if (!rescueLogs.isEmpty() && rescueLogs.get(rescueLogs.size() - 1).worseAfterDose) {
            alertHelper.maybeNotify("worseAfterDose", "Worse after dose", "Child reported feeling worse after rescue.");
        }

        // Inventory low/expired
        mDatabase.child("users").child(childId).child("inventory").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer remaining = snapshot.child("remainingPercent").getValue(Integer.class);
                    Long expiresAt = snapshot.child("expiresAt").getValue(Long.class);
                    if (remaining != null && remaining <= 20) {
                        alertHelper.maybeNotify("inventoryLow", "Inventory low", "Rescue canister is at " + remaining + "%.");
                    }
                    if (expiresAt != null && expiresAt < System.currentTimeMillis()) {
                        alertHelper.maybeNotify("inventoryExpired", "Medication expired", "Expiry date reached. Replace inhaler.");
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Triage escalation listener
        mDatabase.child("users").child(childId).child("triageIncidents")
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String status = child.child("status").getValue(String.class);
                            if ("escalated".equalsIgnoreCase(status)) {
                                alertHelper.maybeNotify("triageEscalation", "Triage escalation", "Escalation flagged in last check-in.");
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    //Overrides for Triage Banner
    /*
    @Override
    protected void onStart() {
        super.onStart();
        startTriageListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopTriageListener();
    }
    */

}
