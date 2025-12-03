package com.example.smartair;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartair.r3.ControllerLogEntry;
import com.example.smartair.r3.RescueLogEntry;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProviderReportActivity extends AppCompatActivity {

    private static final String EXTRA_CHILD_ID = "CHILD_ID";

    private String childId;
    private DatabaseReference mDatabase;
    private TextView tvStatus, tvSharingGuard;
    private EditText etStart, etEnd;
    private Button btnGenerate, btnViewPdf;
    private User childProfile;
    private final Map<String, Boolean> sharingSettings = new HashMap<>();
    private final List<RescueLog> rescueLogs = new ArrayList<>();
    private final List<ControllerLog> controllerLogs = new ArrayList<>();
    private final List<RescueLogEntry> rescueLogEntries = new ArrayList<>();
    private final List<ControllerLogEntry> controllerLogEntries = new ArrayList<>();
    private final List<ZoneSample> zoneSamples = new ArrayList<>();
    private final List<TriageNote> triageNotes = new ArrayList<>();
    private final List<SymptomLog> symptomLogs = new ArrayList<>();
    private final Map<String, Integer> plannedSchedule = new HashMap<>();

    public static void launch(Context context, String childId, int suggestedRangeDays) {
        Intent intent = new Intent(context, ProviderReportActivity.class);
        intent.putExtra(EXTRA_CHILD_ID, childId);
        long now = System.currentTimeMillis();
        long start = now - 90L * 24 * 60 * 60 * 1000;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        intent.putExtra("startPrefill", df.format(new Date(start)));
        intent.putExtra("endPrefill", df.format(new Date(now)));
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_report);

        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        if (childId == null) {
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        tvStatus = findViewById(R.id.tvReportStatus);
        tvSharingGuard = findViewById(R.id.tvSharingGuard);
        etStart = findViewById(R.id.etStartDate);
        etEnd = findViewById(R.id.etEndDate);
        btnGenerate = findViewById(R.id.btnGeneratePdf);
        btnViewPdf = findViewById(R.id.btnViewPdf);

        if (getIntent().hasExtra("startPrefill")) {
            etStart.setText(getIntent().getStringExtra("startPrefill"));
        }
        if (getIntent().hasExtra("endPrefill")) {
            etEnd.setText(getIntent().getStringExtra("endPrefill"));
        }

        btnGenerate.setOnClickListener(v -> fetchAndGenerateReport());
        btnViewPdf.setOnClickListener(v -> openPdf());
        btnViewPdf.setEnabled(new File(getReportFilePath(this, childId)).exists());

        hydrateData();
    }

    private void hydrateData() {
        mDatabase.child("users").child(childId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                childProfile = snapshot.getValue(User.class);
                if (childProfile != null) childProfile.userId = childId;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabase.child("users").child(childId).child("sharingSettings")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        sharingSettings.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Boolean val = child.getValue(Boolean.class);
                            sharingSettings.put(child.getKey(), val != null && val);
                        }
                        boolean isShared = sharingSettings.getOrDefault("isShared", false);
                        if (!isShared) {
                            tvSharingGuard.setText("Sharing is OFF. Report will only include local data when exported by parent.");
                        } else {
                            tvSharingGuard.setText("Sharing is ON. Provider sees only toggled sections.");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        mDatabase.child("users").child(childId).child("rescueLogs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        rescueLogs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            RescueLog log = child.getValue(RescueLog.class);
                            if (log != null) rescueLogs.add(log);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        mDatabase.child("users").child(childId).child("controllerLogs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        controllerLogs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ControllerLog log = child.getValue(ControllerLog.class);
                            if (log != null) controllerLogs.add(log);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        mDatabase.child("users").child(childId).child("zoneHistory")
                .limitToLast(200)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        zoneSamples.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Integer score = child.child("score").getValue(Integer.class);
                            Long ts = child.child("timestamp").getValue(Long.class);
                            if (score != null && ts != null) {
                                zoneSamples.add(new ZoneSample(ts, score));
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        mDatabase.child("users").child(childId).child("triageIncidents")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        triageNotes.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String status = child.child("status").getValue(String.class);
                            String flags = child.child("flags").getValue(String.class);
                            Long ts = child.child("timestamp").getValue(Long.class);
                            triageNotes.add(new TriageNote(ts != null ? ts : System.currentTimeMillis(), status, flags));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        mDatabase.child("users").child(childId).child("symptomLogs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        symptomLogs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            SymptomLog log = child.getValue(SymptomLog.class);
                            if (log != null) symptomLogs.add(log);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        mDatabase.child("users").child(childId).child("plannedSchedule")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        plannedSchedule.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Integer val = child.getValue(Integer.class);
                            if (val != null) plannedSchedule.put(child.getKey(), val);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchAndGenerateReport() {
        final List<RescueLog> rescues = new ArrayList<>();
        final List<ControllerLog> controllers = new ArrayList<>();
        final List<RescueLogEntry> rescueEntries = new ArrayList<>();
        final List<ControllerLogEntry> controllerEntries = new ArrayList<>();
        final List<ZoneSample> zones = new ArrayList<>();
        final List<TriageNote> triage = new ArrayList<>();
        final int[] pending = {7};

        Runnable tryFinish = () -> {
            pending[0]--;
            if (pending[0] == 0) {
                generateReport(rescues, controllers, rescueEntries, controllerEntries, zones, triage);
            }
        };

        mDatabase.child("users").child(childId).child("rescueLogs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        rescues.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            RescueLog log = child.getValue(RescueLog.class);
                            if (log != null) rescues.add(log);
                        }
                        tryFinish.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { tryFinish.run(); }
                });

        mDatabase.child("users").child(childId).child("controllerLogs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        controllers.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ControllerLog log = child.getValue(ControllerLog.class);
                            if (log != null) controllers.add(log);
                        }
                        tryFinish.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { tryFinish.run(); }
                });

        mDatabase.child("users").child(childId).child("zoneHistory")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        zones.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Integer score = child.child("score").getValue(Integer.class);
                            Long ts = child.child("timestamp").getValue(Long.class);
                            if (score != null && ts != null) zones.add(new ZoneSample(ts, score));
                        }
                        tryFinish.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { tryFinish.run(); }
                });

        mDatabase.child("users").child(childId).child("triageIncidents")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        triage.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String status = child.child("status").getValue(String.class);
                            String flags = child.child("flags").getValue(String.class);
                            Long ts = child.child("timestamp").getValue(Long.class);
                            triage.add(new TriageNote(ts != null ? ts : System.currentTimeMillis(), status, flags));
                        }
                        tryFinish.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { tryFinish.run(); }
                });

        mDatabase.child("users").child(childId).child("symptomLogs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        symptomLogs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            SymptomLog log = child.getValue(SymptomLog.class);
                            if (log != null) symptomLogs.add(log);
                        }
                        tryFinish.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { tryFinish.run(); }
                });

        // Dashboard data source: medicine_logs
        DatabaseReference meds = FirebaseDatabase
                .getInstance("https://smartair-a6669-default-rtdb.firebaseio.com")
                .getReference("medicine_logs");

        meds.child("rescue").child(childId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        rescueEntries.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            RescueLogEntry log = child.getValue(RescueLogEntry.class);
                            if (log != null) rescueEntries.add(log);
                        }
                        tryFinish.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { tryFinish.run(); }
                });

        meds.child("controller").child(childId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        controllerEntries.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ControllerLogEntry log = child.getValue(ControllerLogEntry.class);
                            if (log != null) controllerEntries.add(log);
                        }
                        tryFinish.run();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { tryFinish.run(); }
                });
    }

    private void generateReport(List<RescueLog> rescues, List<ControllerLog> controllers,
                                List<RescueLogEntry> rescueEntries, List<ControllerLogEntry> controllerEntries,
                                List<ZoneSample> zones, List<TriageNote> triage) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        long startMillis;
        long endMillis;
        try {
            Date start = df.parse(etStart.getText().toString().trim());
            Date end = df.parse(etEnd.getText().toString().trim());
            if (start == null || end == null) throw new ParseException("Bad date", 0);
            startMillis = start.getTime();
            endMillis = end.getTime();
        } catch (ParseException e) {
            Toast.makeText(this, "Use YYYY-MM-DD for dates", Toast.LENGTH_SHORT).show();
            return;
        }

        long minRange = 90L * 24 * 60 * 60 * 1000;
        long maxRange = 190L * 24 * 60 * 60 * 1000;
        long range = endMillis - startMillis;
        if (range < minRange || range > maxRange) {
            Toast.makeText(this, "Range should be between 3-6 months", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean sharingOn = sharingSettings.getOrDefault("isShared", false);

        List<Long> rescueTs = buildRescueTimestamps(rescueEntries, rescues);
        int windowRescues = countTsInRange(rescueTs, startMillis, endMillis);
        int weeklyRescue = countTsInRange(rescueTs, endMillis - 7L * 24 * 60 * 60 * 1000, endMillis);

        List<ZoneSample> filteredZones = filterZones(zones, startMillis, endMillis);
        List<ZoneSample> dailyZones = buildDailyZoneSamples(filteredZones, startMillis, endMillis);
        if (dailyZones.isEmpty()) {
            int score = childProfile != null ? childProfile.asthmaScore : 0;
            dailyZones.add(new ZoneSample(System.currentTimeMillis(), score));
        }

        long last30Start = endMillis - 30L * 24 * 60 * 60 * 1000;
        double adherence = calculateAdherenceEntries(controllerEntries, plannedSchedule, last30Start, endMillis);
        if (adherence < 0) {
            adherence = calculateAdherence(controllers, last30Start, endMillis);
        }

        int symptomBurden = countSymptomBurden(symptomLogs, startMillis, endMillis);
        int notable = countEscalations(triage, startMillis, endMillis);

        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(16f);

        int y = 40;
        paint.setTextSize(20f);
        canvas.drawText("SMART AIR Provider Report", 40, y, paint);
        paint.setTextSize(14f);
        y += 24;
        canvas.drawText("Child: " + (childProfile != null ? childProfile.firstName : childId), 40, y, paint);
        y += 18;
        canvas.drawText("Window: " + etStart.getText() + " to " + etEnd.getText(), 40, y, paint);
        y += 24;

        if (!sharingOn) {
            paint.setColor(Color.RED);
            canvas.drawText("Sharing OFF - parent-controlled export only", 40, y, paint);
            paint.setColor(Color.BLACK);
            y += 24;
        }

        canvas.drawText("Rescue uses (window): " + windowRescues, 40, y, paint); y += 18;
        canvas.drawText("Rescue uses (last 7 days): " + weeklyRescue, 40, y, paint); y += 18;
        canvas.drawText("Controller adherence (last 30 days): " + String.format(Locale.getDefault(), "%.0f%%", adherence), 40, y, paint); y += 18;
        canvas.drawText("Symptom burden (problem days): " + symptomBurden, 40, y, paint); y += 18;
        canvas.drawText("Notable triage incidents: " + notable, 40, y, paint); y += 18;

        y += 12;
        canvas.drawText("Zone distribution", 40, y, paint); y += 10;
        drawZoneBars(canvas, y, dailyZones);
        y += 140;

        canvas.drawText("Rescue frequency (last 30 days)", 40, y, paint); y += 10;
        drawRescueSeries(canvas, y, rescueTs, endMillis);

        doc.finishPage(page);

        File out = new File(getReportFilePath(this, childId));
        try (FileOutputStream fos = new FileOutputStream(out)) {
            doc.writeTo(fos);
            tvStatus.setText("Report saved: " + out.getAbsolutePath());
            btnViewPdf.setEnabled(true);
            Map<String, Object> meta = new HashMap<>();
            meta.put("generatedAt", System.currentTimeMillis());
            meta.put("rescueCount", windowRescues);
            meta.put("adherencePercent", adherence);
            mDatabase.child("reports").child(childId).setValue(meta);
        } catch (IOException e) {
            tvStatus.setText("Failed to save report: " + e.getMessage());
        } finally {
            doc.close();
        }
    }

    private void openPdf() {
        String path = getReportFilePath(this, childId);
        File f = new File(path);
        if (!f.exists()) {
            Toast.makeText(this, "Generate a report first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, PdfPreviewActivity.class);
        intent.putExtra(PdfPreviewActivity.EXTRA_PATH, path);
        startActivity(intent);
    }

    public static String getReportFilePath(Context context, String childId) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "provider_report_" + childId + ".pdf").getAbsolutePath();
    }

    private List<ZoneSample> filterZones(List<ZoneSample> source, long start, long end) {
        List<ZoneSample> list = new ArrayList<>();
        for (ZoneSample sample : source) {
            if (sample.timestamp >= start && sample.timestamp <= end) list.add(sample);
        }
        return list;
    }

    private List<ZoneSample> buildDailyZoneSamples(List<ZoneSample> source, long start, long end) {
        Map<String, ZoneSample> perDay = new HashMap<>();
        for (ZoneSample sample : source) {
            if (sample.timestamp < start || sample.timestamp > end) continue;
            String key = dayKey(sample.timestamp);
            ZoneSample existing = perDay.get(key);
            if (existing == null || sample.timestamp > existing.timestamp) {
                perDay.put(key, sample);
            }
        }
        return new ArrayList<>(perDay.values());
    }

    private double calculateAdherence(List<ControllerLog> controllers, long start, long end) {
        if (childProfile == null) return 0;
        int perDay = Math.max(1, childProfile.plannedControllerPerDay);
        int perWeek = Math.max(1, childProfile.plannedControllerDaysPerWeek);

        Map<String, Integer> dailyCount = new HashMap<>();
        for (ControllerLog log : controllers) {
            if (log.timestamp < start || log.timestamp > end) continue;
            String key = dayKey(log.timestamp);
            dailyCount.put(key, dailyCount.getOrDefault(key, 0) + log.doses);
        }

        int windowDays = (int) Math.max(1, Math.round((end - start) / (24 * 60 * 60 * 1000.0)));
        int plannedDays = Math.max(1, (int) Math.round((perWeek / 7.0) * windowDays));
        int completedDays = 0;
        for (Integer doses : dailyCount.values()) {
            if (doses >= perDay) completedDays++;
        }
        return Math.min(100, (completedDays / (double) plannedDays) * 100);
    }

    private double calculateAdherenceEntries(List<ControllerLogEntry> entries, Map<String, Integer> plan, long start, long end) {
        // Mirror parent dashboard monthly adherence: last 30 days, planned defaults to 0, days without a plan count as compliant
        if (entries.isEmpty()) return -1;
        Map<String, Integer> daily = new HashMap<>();
        for (ControllerLogEntry e : entries) {
            long ts = parseEntryTs(e.timestamp);
            if (ts < start || ts > end) continue;
            String key = dayKey(ts);
            daily.put(key, daily.getOrDefault(key, 0) + e.doseCount);
        }
        if (plan == null) plan = new HashMap<>();

        int plannedDays = 0;
        int compliant = 0;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(end);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 30; i++) {
            long t = cal.getTimeInMillis() - i * 24L * 60 * 60 * 1000;
            String dayStr = getDayString(cal.get(Calendar.DAY_OF_WEEK));
            int planned = plan.getOrDefault(dayStr, 0);
            plannedDays++;
            int actual = daily.getOrDefault(dayKey(t), 0);
            if (actual >= planned) compliant++;
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        if (plannedDays == 0) return -1;
        return Math.min(100, (compliant / (double) plannedDays) * 100);
    }

    private int countSymptomBurden(List<SymptomLog> symptoms, long start, long end) {
        int count = 0;
        for (SymptomLog log : symptoms) {
            if (log.timestamp >= start && log.timestamp <= end && log.severity > 0) {
                count++;
            }
        }
        return count;
    }

    private int countEscalations(List<TriageNote> triage, long start, long end) {
        int count = 0;
        for (TriageNote note : triage) {
            if (note.timestamp >= start && note.timestamp <= end && "escalated".equalsIgnoreCase(note.status)) {
                count++;
            }
        }
        return count;
    }

    private List<Long> buildRescueTimestamps(List<RescueLogEntry> entries, List<RescueLog> legacy) {
        List<Long> out = new ArrayList<>();
        for (RescueLogEntry e : entries) {
            long ts = parseEntryTs(e.timestamp);
            if (ts > 0) out.add(ts);
        }
        for (RescueLog log : legacy) {
            out.add(log.timestamp);
        }
        return out;
    }

    private int countTsInRange(List<Long> tsList, long start, long end) {
        int count = 0;
        for (Long ts : tsList) {
            if (ts >= start && ts <= end) count++;
        }
        return count;
    }

    private void drawZoneBars(Canvas canvas, int yStart, List<ZoneSample> samples) {
        int green = 0, yellow = 0, red = 0;
        for (ZoneSample sample : samples) {
            if (sample.score >= 80) green++;
            else if (sample.score >= 50) yellow++;
            else red++;
        }
        int total = Math.max(1, green + yellow + red);
        int baseX = 40;
        int barWidth = 80;
        Paint p = new Paint();

        float greenHeight = Math.max(4f, 100 - (green * 100f / total));
        float yellowHeight = Math.max(4f, 100 - (yellow * 100f / total));
        float redHeight = Math.max(4f, 100 - (red * 100f / total));

        p.setColor(Color.parseColor("#4CAF50"));
        canvas.drawRect(baseX, yStart + greenHeight, baseX + barWidth, yStart + 100, p);
        canvas.drawText("Green", baseX, yStart + 120, p);

        p.setColor(Color.parseColor("#FFC107"));
        canvas.drawRect(baseX + 100, yStart + yellowHeight, baseX + 100 + barWidth, yStart + 100, p);
        canvas.drawText("Yellow", baseX + 100, yStart + 120, p);

        p.setColor(Color.parseColor("#F44336"));
        canvas.drawRect(baseX + 200, yStart + redHeight, baseX + 200 + barWidth, yStart + 100, p);
        canvas.drawText("Red", baseX + 200, yStart + 120, p);
    }

    private void drawRescueSeries(Canvas canvas, int yStart, List<Long> tsList, long endMillis) {
        int days = 30;
        long dayMs = 24L * 60 * 60 * 1000;
        List<Integer> daily = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            long start = endMillis - (i + 1) * dayMs;
            long end = endMillis - i * dayMs;
            int count = 0;
            for (Long ts : tsList) {
                if (ts >= start && ts < end) count++;
            }
            daily.add(count);
        }

        Paint axis = new Paint();
        axis.setColor(Color.parseColor("#BDBDBD"));
        axis.setStrokeWidth(2f);

        Paint line = new Paint();
        line.setColor(Color.parseColor("#1E88E5"));
        line.setStrokeWidth(4f);
        line.setStyle(Paint.Style.STROKE);
        line.setAntiAlias(true);

        Paint dot = new Paint();
        dot.setColor(Color.parseColor("#1E88E5"));
        dot.setStyle(Paint.Style.FILL);

        int width = 480;
        int height = 140;
        int baseX = 40;
        int baseY = yStart + height;

        canvas.drawLine(baseX, yStart, baseX, baseY, axis);
        canvas.drawLine(baseX, baseY, baseX + width, baseY, axis);

        int max = 1;
        for (int v : daily) if (v > max) max = v;

        float prevX = baseX;
        float prevY = baseY - (daily.get(0) / (float) max) * height;
        for (int i = 1; i < daily.size(); i++) {
            float x = baseX + (width / (float) Math.max(1, daily.size() - 1)) * i;
            float y = baseY - (daily.get(i) / (float) max) * height;
            canvas.drawLine(prevX, prevY, x, y, line);
            prevX = x;
            prevY = y;
        }
        for (int i = 0; i < daily.size(); i++) {
            float x = baseX + (width / (float) Math.max(1, daily.size() - 1)) * i;
            float y = baseY - (daily.get(i) / (float) max) * height;
            canvas.drawCircle(x, y, 5f, dot);
        }
    }

    private long parseEntryTs(String ts) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(ts).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    private String dayKey(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
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

    static class ZoneSample {
        long timestamp;
        int score;
        ZoneSample(long timestamp, int score) {
            this.timestamp = timestamp;
            this.score = score;
        }
    }

    static class TriageNote {
        long timestamp;
        String status;
        String flags;
        TriageNote(long timestamp, String status, String flags) {
            this.timestamp = timestamp;
            this.status = status != null ? status : "";
            this.flags = flags != null ? flags : "";
        }
    }
}
