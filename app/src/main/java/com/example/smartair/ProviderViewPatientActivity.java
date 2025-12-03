package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.example.smartair.r3.ControllerLogEntry;
import com.example.smartair.r3.RescueLogEntry;

public class ProviderViewPatientActivity extends AppCompatActivity {

    private String childId;
    private DatabaseReference mDatabase;
    private TrendChartView trendChartView;
    private TextView tvTitle, tvSharingStatus, tvReportMeta, tvRescueStats, tvAdherenceStats, tvTriageStats;
    private Button btnViewPdf;
    private final List<RescueLogEntry> rescueLogs = new ArrayList<>();
    private final List<ControllerLogEntry> controllerLogs = new ArrayList<>();
    private final List<ZoneEntry> zoneEntries = new ArrayList<>();
    private final List<ProviderReportActivity.TriageNote> triageNotes = new ArrayList<>();
    private final Map<String, Boolean> sharingSettings = new HashMap<>();
    private final Map<String, Integer> plannedSchedule = new HashMap<>();
    private User childProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_view_patient);

        childId = getIntent().getStringExtra("CHILD_ID");
        if (childId == null) {
            Toast.makeText(this, "No patient id provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        loadProfile();
        loadSharing();
        listenForLogs();
        loadReportMeta();

        btnViewPdf.setOnClickListener(v -> tryOpenPdf());
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvPatientTitle);
        tvSharingStatus = findViewById(R.id.tvSharingStatus);
        tvReportMeta = findViewById(R.id.tvReportMeta);
        tvRescueStats = findViewById(R.id.tvRescueStats);
        tvAdherenceStats = findViewById(R.id.tvAdherenceStats);
        tvTriageStats = findViewById(R.id.tvTriageStats);
        trendChartView = findViewById(R.id.providerTrendChart);
        btnViewPdf = findViewById(R.id.btnViewSharedPdf);
    }

    private void loadProfile() {
        mDatabase.child("users").child(childId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childProfile = snapshot.getValue(User.class);
                if (childProfile != null) {
                    childProfile.userId = childId;
                    String name = (childProfile.firstName != null ? childProfile.firstName : "Child") +
                            (childProfile.lastName != null ? " " + childProfile.lastName : "");
                    tvTitle.setText(name);
                    
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
                    refreshViews();
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
                        tvSharingStatus.setText(isShared ? "Parent has sharing ON for this child." : "Parent sharing is OFF. Data is hidden.");
                        refreshViews();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void listenForLogs() {
        // Use medicine_logs for parity with parent dashboard and PDF report
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
                        refreshViews();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        medicineLogsRef.child("controller").child(childId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        controllerLogs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ControllerLogEntry log = child.getValue(ControllerLogEntry.class);
                            if (log != null) controllerLogs.add(log);
                        }
                        refreshViews();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        mDatabase.child("users").child(childId).child("zoneHistory")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        zoneEntries.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ZoneEntry entry = child.getValue(ZoneEntry.class);
                            if (entry != null) zoneEntries.add(entry);
                        }
                        refreshViews();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        mDatabase.child("users").child(childId).child("triageIncidents")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        triageNotes.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String status = child.child("status").getValue(String.class);
                            String flags = child.child("flags").getValue(String.class);
                            Long ts = child.child("timestamp").getValue(Long.class);
                            triageNotes.add(new ProviderReportActivity.TriageNote(ts != null ? ts : System.currentTimeMillis(), status, flags));
                        }
                        refreshViews();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadReportMeta() {
        mDatabase.child("reports").child(childId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long generatedAt = snapshot.child("generatedAt").getValue(Long.class);
                    Integer rescueCount = snapshot.child("rescueCount").getValue(Integer.class);
                    Double adherence = snapshot.child("adherencePercent").getValue(Double.class);
                    DateFormat df = SimpleDateFormat.getDateTimeInstance();
                    String when = generatedAt != null ? df.format(new Date(generatedAt)) : "unknown time";
                    tvReportMeta.setText("Last report generated: " + when +
                            " | Rescues: " + (rescueCount != null ? rescueCount : 0) +
                            " | Adherence: " + (adherence != null ? String.format(Locale.getDefault(), "%.0f%%", adherence) : "--"));
                } else {
                    tvReportMeta.setText("No report generated yet.");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void refreshViews() {
        boolean isShared = sharingSettings.getOrDefault("isShared", false);
        if (!isShared) {
            tvRescueStats.setText("Rescue data hidden (sharing off).");
            tvAdherenceStats.setText("Controller data hidden (sharing off).");
            tvTriageStats.setText("Triage data hidden (sharing off).");
            trendChartView.setData(new ArrayList<>(), 7);
            return;
        }

        // Rescue
        if (sharingSettings.getOrDefault("shareRescue", false)) {
            int weekly = countRescuesInDays(7);
            String last = rescueLogs.isEmpty() ? "None logged" : DateFormat.getDateTimeInstance().format(new Date(latestRescueTs()));
            tvRescueStats.setText("Last rescue: " + last + " | 7-day count: " + weekly);
        } else {
            tvRescueStats.setText("Rescue data not shared.");
        }

        // Adherence
        if (sharingSettings.getOrDefault("shareController", false)) {
            double adherence = calculateAdherence();
            tvAdherenceStats.setText("Controller adherence: " + String.format(Locale.getDefault(), "%.0f%%", adherence));
        } else {
            tvAdherenceStats.setText("Controller data not shared.");
        }

        // Triage
        if (sharingSettings.getOrDefault("shareTriage", false)) {
            int escalations = 0;
            for (ProviderReportActivity.TriageNote note : triageNotes) {
                if ("escalated".equalsIgnoreCase(note.status)) escalations++;
            }
            tvTriageStats.setText("Triage incidents: " + triageNotes.size() + " (" + escalations + " escalated)");
        } else {
            tvTriageStats.setText("Triage incidents not shared.");
        }

        // Trend chart (rescue frequency)
        if (sharingSettings.getOrDefault("shareCharts", false) || sharingSettings.getOrDefault("shareRescue", false)) {
            trendChartView.setData(buildDailyCounts(30), 30);
        } else {
            trendChartView.setData(new ArrayList<>(), 30);
        }
    }

    private int countRescuesInDays(int days) {
        int count = 0;
        long now = System.currentTimeMillis();
        long window = days * 24L * 60 * 60 * 1000;
        for (RescueLogEntry log : rescueLogs) {
            long ts = parseTimestamp(log.timestamp);
            if (ts > 0 && now - ts <= window) count++;
        }
        return count;
    }

    private long latestRescueTs() {
        long latest = 0;
        for (RescueLogEntry log : rescueLogs) {
            long ts = parseTimestamp(log.timestamp);
            if (ts > latest) latest = ts;
        }
        return latest;
    }

    private List<Integer> buildDailyCounts(int days) {
        long now = System.currentTimeMillis();
        long dayMs = 24L * 60 * 60 * 1000;
        List<Integer> counts = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            long start = now - (i + 1) * dayMs;
            long end = now - i * dayMs;
            int c = 0;
            for (RescueLogEntry log : rescueLogs) {
                long ts = parseTimestamp(log.timestamp);
                if (ts >= start && ts < end) c++;
            }
            counts.add(c);
        }
        return counts;
    }

    private double calculateAdherence() {
        if (childProfile == null) return 0;
        
        long now = System.currentTimeMillis();
        long thirtyDaysMs = 30L * 24 * 60 * 60 * 1000;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        
        Map<String, Integer> dailyCount = new HashMap<>();
        for (ControllerLogEntry log : controllerLogs) {
            if (log.timestamp == null) continue;
            try {
                Date date = sdf.parse(log.timestamp);
                if (date == null) continue;
                long time = date.getTime();
                
                if (now - time > thirtyDaysMs) continue;
                
                String key = dayKey(time);
                dailyCount.put(key, dailyCount.getOrDefault(key, 0) + log.doseCount);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        
        int plannedDays = 0;
        int completedDays = 0;
        Calendar cal = Calendar.getInstance();
        
        for (int i = 0; i < 30; i++) {
            long time = now - (i * 24L * 60 * 60 * 1000);
            cal.setTimeInMillis(time);
            String dayStr = getDayString(cal.get(Calendar.DAY_OF_WEEK));
            
            int planned = plannedSchedule.getOrDefault(dayStr, 0);
            if (planned > 0) {
                plannedDays++;
                String key = dayKey(time);
                int actual = dailyCount.getOrDefault(key, 0);
                if (actual >= planned) completedDays++;
            }
        }
        
        if (plannedDays == 0) return 100;
        
        double adherence = (completedDays / (double) plannedDays) * 100;
        return Math.min(100, adherence);
    }

    private String dayKey(long ts) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts);
        return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.DAY_OF_YEAR);
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

    private void tryOpenPdf() {
        File f = new File(ProviderReportActivity.getReportFilePath(this, childId));
        if (f.exists()) {
            Intent intent = new Intent(this, PdfPreviewActivity.class);
            intent.putExtra(PdfPreviewActivity.EXTRA_PATH, f.getAbsolutePath());
            startActivity(intent);
        } else {
            Toast.makeText(this, "No shared PDF on this device. Ask parent to export and share.", Toast.LENGTH_SHORT).show();
        }
    }

    private long parseTimestamp(String ts) {
        if (ts == null) return 0;
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(ts).getTime();
        } catch (Exception e) {
            return 0;
        }
    }
}
