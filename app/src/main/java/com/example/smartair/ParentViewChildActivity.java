package com.example.smartair;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.content.pm.PackageManager;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import com.example.smartair.R4.model.TriageIncident;

import com.example.smartair.R4.triage.ActionPlanActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.DateFormat;
import java.text.ParseException;
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
import com.example.smartair.r3.MedicineLogActivity;
import com.example.smartair.r3.MedicineLogFragment;
import com.example.smartair.r3.InventoryLogActivity;
import com.example.smartair.r3.BadgeSettingsActivity;
import com.example.smartair.r3.ControllerLogEntry;
import com.example.smartair.r3.RescueLogEntry;
import com.example.smartair.r3.InventoryItem;
import com.google.firebase.database.ChildEventListener;

public class ParentViewChildActivity extends AppCompatActivity {

    private String childId;
    private DatabaseReference mDatabase;

    private TextView tvTitle, tvSharingTag, tvZoneValue, tvZoneSubtitle, tvLastRescue, tvWeeklyCount, tvAdherenceWeekly, tvAdherenceMonthly, tvAdherencePlan, tvReminderMessage;
    private View cardZone, cardRescue, cardTrend, cardAdherence;
    private LinearLayout remindersLayout;
    private TrendChartView trendChartView;
    private Button btnRange7, btnRange30, btnGenerateReport, btnQuickRescue, btnQuickController, btnViewInventory, btnBadgeSettings, btnViewReport, btnExpandTrend;
    private Map<String, Boolean> sharingSettings = new HashMap<>();

    private final List<RescueLogEntry> rescueLogs = new ArrayList<>();
    private final List<ControllerLogEntry> controllerLogs = new ArrayList<>();
    private User childProfile;
    private Map<String, Integer> plannedSchedule = new HashMap<>();

    private int selectedRangeDays = 7;
    private AlertHelper alertHelper;

    private Button btnSetPb;

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


        createTriageChannel();
        bindViews();
        wireActions();
        loadProfile();
        loadSharing();
        listenForLogs();
        checkInventoryReminders();
        listenForTriageUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (childProfile != null) {
            updateAdherenceTile();
            checkAlerts();
        }
        checkInventoryReminders();
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvChildTitle);
        tvSharingTag = findViewById(R.id.tvSharingTag);
        tvZoneValue = findViewById(R.id.tvZoneValue);
        tvZoneSubtitle = findViewById(R.id.tvZoneSubtitle);
        tvLastRescue = findViewById(R.id.tvLastRescue);
        tvWeeklyCount = findViewById(R.id.tvWeeklyCount);
        tvAdherenceWeekly = findViewById(R.id.tvAdherenceWeekly);
        tvAdherenceMonthly = findViewById(R.id.tvAdherenceMonthly);
        tvAdherencePlan = findViewById(R.id.tvAdherencePlan);

        remindersLayout = findViewById(R.id.remindersLayout);
        tvReminderMessage = findViewById(R.id.tvReminderMessage);

        trendChartView = findViewById(R.id.trendChart);
        btnRange7 = findViewById(R.id.btnRange7);
        btnRange30 = findViewById(R.id.btnRange30);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        btnQuickRescue = findViewById(R.id.btnQuickRescue);
        btnQuickController = findViewById(R.id.btnQuickController);
        btnViewInventory = findViewById(R.id.btnViewInventory);
        btnBadgeSettings = findViewById(R.id.btnBadgeSettings);
        btnViewReport = findViewById(R.id.btnViewReport);
        btnExpandTrend = findViewById(R.id.btnExpandTrend);
        btnSetPb = findViewById(R.id.btn_set_pb);

        // Tiles
        cardZone = findViewById(R.id.cardZone);
        cardRescue = findViewById(R.id.cardRescue);
        cardTrend = findViewById(R.id.cardTrend);
        cardAdherence = findViewById(R.id.cardAdherence);
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

        btnQuickRescue.setOnClickListener(v -> {
            Intent intent = new Intent(this, MedicineLogActivity.class);
            intent.putExtra("CHILD_ID", childId);
            intent.putExtra(MedicineLogFragment.ARG_TYPE, "RESCUE");
            intent.putExtra(MedicineLogFragment.ARG_ROLE, "Parent");
            startActivity(intent);
        });

        btnQuickController.setOnClickListener(v -> {
            Intent intent = new Intent(this, MedicineLogActivity.class);
            intent.putExtra("CHILD_ID", childId);
            intent.putExtra(MedicineLogFragment.ARG_TYPE, "CONTROLLER");
            intent.putExtra(MedicineLogFragment.ARG_ROLE, "Parent");
            startActivity(intent);
        });

        btnViewInventory.setOnClickListener(v -> {
            Intent intent = new Intent(this, InventoryLogActivity.class);
            intent.putExtra("CHILD_ID", childId);
            startActivity(intent);
        });

        btnBadgeSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, BadgeSettingsActivity.class);
            intent.putExtra("CHILD_ID", childId);
            startActivity(intent);
        });

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

        // cardRescue click listener removed

        cardAdherence.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdherenceSummaryActivity.class);
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

        Button btnEditPlan = findViewById(R.id.btnEditActionPlan);
        btnEditPlan.setOnClickListener(v -> {
            Intent i = new Intent(ParentViewChildActivity.this, ActionPlanActivity.class);
            i.putExtra("CHILD_ID", childId);
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

                    plannedSchedule.clear();
                    DataSnapshot schedSnap = snapshot.child("plannedSchedule");
                    if (schedSnap.exists()) {
                        for (DataSnapshot d : schedSnap.getChildren()) {
                            String day = d.getKey();
                            Integer val = d.getValue(Integer.class);
                            if (day != null && val != null) {
                                plannedSchedule.put(day, val);
                            }
                        }
                    }

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
        // Modified to listen to medicine_logs/rescue
        DatabaseReference medicineLogsRef = FirebaseDatabase
                .getInstance("https://smartair-a6669-default-rtdb.firebaseio.com")
                .getReference("medicine_logs");

        medicineLogsRef.child("rescue").child(childId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        rescueLogs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            RescueLogEntry log = child.getValue(RescueLogEntry.class);
                            if (log != null) rescueLogs.add(log);
                        }
                        updateRescueTile();
                        refreshTrend();
                        checkAlerts();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Controller logs
        medicineLogsRef.child("controller").child(childId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        controllerLogs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ControllerLogEntry log = child.getValue(ControllerLogEntry.class);
                            if (log != null) controllerLogs.add(log);
                        }
                        updateAdherenceTile();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

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

        RescueLogEntry latest = getLatestRescueLog();

        DateFormat df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, Locale.getDefault());
        long ts = parseTimestamp(latest.timestamp);
        if (ts > 0) {
            tvLastRescue.setText(df.format(new Date(ts)));
        } else {
            tvLastRescue.setText(latest.timestamp);
        }

        int weekly = countRescuesInDays(7);
        tvWeeklyCount.setText("Weekly count: " + weekly);
    }

    private RescueLogEntry getLatestRescueLog() {
        if (rescueLogs.isEmpty()) return null;
        RescueLogEntry latest = rescueLogs.get(0);
        long latestTs = parseTimestamp(latest.timestamp);

        for (RescueLogEntry log : rescueLogs) {
            long ts = parseTimestamp(log.timestamp);
            if (ts > latestTs) {
                latest = log;
                latestTs = ts;
            }
        }
        return latest;
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
            for (RescueLogEntry log : rescueLogs) {
                long ts = parseTimestamp(log.timestamp);
                if (ts >= start && ts < end) count++;
            }
            counts.add(count);
        }
        return counts;
    }

    private int countRescuesInDays(int days) {
        long now = System.currentTimeMillis();
        long range = days * 24L * 60 * 60 * 1000;
        int count = 0;
        for (RescueLogEntry log : rescueLogs) {
            long ts = parseTimestamp(log.timestamp);
            if (now - ts <= range) count++;
        }
        return count;
    }

    private void updateAdherenceTile() {
        if (childProfile == null) return;

        StringBuilder sb = new StringBuilder("Plan: ");
        if (plannedSchedule.isEmpty()) {
            sb.append("No schedule set");
        } else {
            sb.append("Custom Schedule");
        }
        tvAdherencePlan.setText(sb.toString());

        long now = System.currentTimeMillis();
        // Time parts reset for today
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(now);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Map<String, Integer> dailyCount = new HashMap<>();
        for (ControllerLogEntry log : controllerLogs) {
            if (log.timestamp == null) continue;
            try {
                Date date = sdf.parse(log.timestamp);
                if (date == null) continue;
                String dayKey = dayKey(date.getTime());
                int current = dailyCount.containsKey(dayKey) ? dailyCount.get(dayKey) : 0;
                dailyCount.put(dayKey, current + log.doseCount);
            } catch (ParseException e) {}
        }

        // Weekly Calculation (Current Week: Sun -> Today)
        double weeklyAdherence = calculateAdherenceForWeek(today, dailyCount);
        tvAdherenceWeekly.setText(String.format(Locale.getDefault(), "%.0f%%", weeklyAdherence));

        // Monthly Calculation (Last 30 Days)
        double monthlyAdherence = calculateAdherenceForLast30Days(today, dailyCount);
        tvAdherenceMonthly.setText(String.format(Locale.getDefault(), "%.0f%%", monthlyAdherence));
    }

    private double calculateAdherenceForWeek(Calendar today, Map<String, Integer> dailyCount) {
        Calendar cal = (Calendar) today.clone();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        if (cal.after(today)) cal.add(Calendar.DAY_OF_YEAR, -7);

        int plannedDays = 0;
        int compliantDays = 0;

        Calendar iter = (Calendar) cal.clone();
        for (int i = 0; i < 7; i++) {
            if (iter.after(today)) break;

            int dayOfWeek = iter.get(Calendar.DAY_OF_WEEK);
            String dayStr = getDayString(dayOfWeek);
            int planned = plannedSchedule.getOrDefault(dayStr, 0);

            plannedDays++;
            String dKey = dayKey(iter.getTimeInMillis());
            int actual = dailyCount.getOrDefault(dKey, 0);
            if (actual >= planned) compliantDays++;

            iter.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (plannedDays == 0) return 100;
        return ((double) compliantDays / plannedDays) * 100;
    }

    private double calculateAdherenceForLast30Days(Calendar today, Map<String, Integer> dailyCount) {
        int plannedDays = 0;
        int compliantDays = 0;

        Calendar iter = (Calendar) today.clone();
        iter.add(Calendar.DAY_OF_YEAR, -29);

        for (int i = 0; i < 30; i++) {
            int dayOfWeek = iter.get(Calendar.DAY_OF_WEEK);
            String dayStr = getDayString(dayOfWeek);
            int planned = plannedSchedule.getOrDefault(dayStr, 0);

            plannedDays++;
            String dKey = dayKey(iter.getTimeInMillis());
            int actual = dailyCount.getOrDefault(dKey, 0);
            if (actual >= planned) compliantDays++;

            iter.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (plannedDays == 0) return 100;
        return ((double) compliantDays / plannedDays) * 100;
    }

    private String getDayString(int calendarDay) {
        switch (calendarDay) {
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

    private String dayKey(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int y = cal.get(Calendar.YEAR);
        int d = cal.get(Calendar.DAY_OF_YEAR);
        return y + "-" + d;
    }

    // Keeping for now but not used by button anymore
    private void showControllerLogDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_log_controller, null);
        EditText etDose = dialogView.findViewById(R.id.etControllerDose);

        new AlertDialog.Builder(this)
                .setTitle("Log Controller Dose")
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    int dose = parseInt(etDose.getText().toString(), 1);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    String timestamp = sdf.format(new Date());

                    ControllerLogEntry log = new ControllerLogEntry("Manual Entry", dose, timestamp);

                    DatabaseReference logsRef = FirebaseDatabase
                            .getInstance("https://smartair-a6669-default-rtdb.firebaseio.com")
                            .getReference("medicine_logs");

                    logsRef.child("controller").child(childId).push().setValue(log);

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
            RescueLogEntry log = rescueLogs.get(i);
            long ts = parseTimestamp(log.timestamp);
            if (now - ts <= 3 * 60 * 60 * 1000) rapidCount++;
        }
        if (rapidCount >= 3) {
            alertHelper.maybeNotify("rapidRescue", "Rapid rescue repeats", "3+ rescue uses in 3 hours.");
        }

        // Worse feeling from Inhaler Guide
        mDatabase.child("guide_stats").child(childId).child("pendingNotifications").child("worse_feeling")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                            alertHelper.maybeNotify("worseFeeling", "Worse after dose", "Child reported feeling worse after inhaler usage.");
                            snapshot.getRef().removeValue();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

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

    private void checkInventoryReminders() {
        DatabaseReference inventoryRef = FirebaseDatabase.getInstance().getReference("inventory");

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> messages = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                // Clear time for date comparison consistency
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                Date today = cal.getTime();
                cal.add(Calendar.DAY_OF_YEAR, 7);
                Date nextWeek = cal.getTime();

                // Check Controller
                checkItems(snapshot.child("controller").child(childId), messages, today, nextWeek);
                // Check Rescue
                checkItems(snapshot.child("rescue").child(childId), messages, today, nextWeek);

                if (!messages.isEmpty()) {
                    remindersLayout.setVisibility(View.VISIBLE);
                    StringBuilder sb = new StringBuilder();
                    for (String msg : messages) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append("• ").append(msg);
                    }
                    TextView tv = findViewById(R.id.tvReminderMessage);
                    tv.setText(sb.toString());
                } else {
                    remindersLayout.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        inventoryRef.addListenerForSingleValueEvent(listener);
    }

    private void checkItems(DataSnapshot itemsSnap, List<String> messages, Date today, Date nextWeek) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (DataSnapshot itemSnap : itemsSnap.getChildren()) {
            InventoryItem item = itemSnap.getValue(InventoryItem.class);
            if (item != null) {
                if (item.percentLeft <= 0) {
                    messages.add("Inhaler " + item.name + " is empty!");
                } else if (item.isLow()) {
                    messages.add("Inhaler " + item.name + " is low (" + item.percentLeft + "%).");
                }

                if (item.expiryDate != null) {
                    try {
                        Date exp = sdf.parse(item.expiryDate);
                        if (exp != null) {
                            if (exp.compareTo(today) <= 0) {
                                messages.add("Inhaler " + item.name + " has expired!");
                            } else if (exp.compareTo(nextWeek) <= 0) {
                                messages.add("Inhaler " + item.name + " expires soon (" + item.expiryDate + ").");
                            }
                        }
                    } catch (ParseException e) {
                        // Fallback or ignore
                    }
                }
            }
        }
    }

    //Triage Notification
    private static final String TRIAGE_CHANNEL = "triage_alerts";

    private void createTriageChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    TRIAGE_CHANNEL,
                    "Triage Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Notifications for triage updates");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }
    private void listenForTriageUpdates() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("children_triage_incidents")
                .child(childId);

        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snap, String prev) {
                TriageIncident inc = snap.getValue(TriageIncident.class);
                if (inc != null) {
                    showTriageNotification("New triage session started");
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snap, String prev) {
                TriageIncident inc = snap.getValue(TriageIncident.class);
                if (inc != null && Boolean.TRUE.equals(inc.escalated)) {
                    showTriageNotification("Triage escalation detected!");
                }
            }

            @Override public void onChildRemoved(@NonNull DataSnapshot snap) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snap, String prev) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showTriageNotification(String message) {

        // Android 13+ permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return; // Don’t crash; silently skip
            }
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, TRIAGE_CHANNEL)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle("Triage Update")
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        // Unique ID
        int notifId = (int) (System.currentTimeMillis() & 0xfffffff);

        try {
            NotificationManagerCompat.from(this).notify(notifId, builder.build());
        } catch (SecurityException ignored) {
            // Happens if no permission — ignore
        }
    }



    private long parseTimestamp(String timestamp) {
        if (timestamp == null) return 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date d = sdf.parse(timestamp);
            return d != null ? d.getTime() : 0;
        } catch (ParseException e) {
            return 0;
        }
    }
}