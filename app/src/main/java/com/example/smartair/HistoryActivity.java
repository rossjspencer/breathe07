package com.example.smartair;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private TextView tvTitle;
    private ImageButton btnFilter;
    private Button btnExport;
    private DatabaseReference mDatabase;
    private String childId;
    private String childName = "";

    private List<SymptomLog> allLogs = new ArrayList<>();
    private List<SymptomLog> filteredLogs = new ArrayList<>();

    // Filter State
    private int filterMonths = 3; // Default 3 months
    private boolean[] selectedSymptoms; // Tracks checkbox state
    private boolean[] selectedTriggers; // Tracks checkbox state

    private final String[] SYMPTOM_OPTIONS = {"Cough", "Wheezing", "Shortness of Breath", "Chest Tightness", "Night Waking"};
    private final String[] TRIGGER_OPTIONS = {"Dust", "Pollen", "Smoke", "Exercise", "Pets", "Cold air", "Illness", "Perfumes/Cleaners/Strong Odors"};
    
    // NEW: Flag to check if child is viewing their own logs
    private boolean isChildViewingOwnLogs = false;
    private String userRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mDatabase = FirebaseDatabase.getInstance().getReference("logs");

        // Init arrays
        selectedSymptoms = new boolean[SYMPTOM_OPTIONS.length];
        selectedTriggers = new boolean[TRIGGER_OPTIONS.length];

        // Get Extras
        if (getIntent().hasExtra("CHILD_ID")) {
            childId = getIntent().getStringExtra("CHILD_ID");
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            childId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        
        if (getIntent().hasExtra("CHILD_NAME")) {
            childName = getIntent().getStringExtra("CHILD_NAME");
        }
        
        // NEW: Determine viewer type
        // If no Firebase Auth user, it is a Child using custom login.
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            isChildViewingOwnLogs = true;
        } else {
            // If logged in, verify role just in case
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
            userRef.child("role").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        userRole = snapshot.getValue(String.class);
                        // If the user is a Child (but using Auth), enforce the filter
                        if ("Child".equals(userRole)) {
                            isChildViewingOwnLogs = true;
                        }
                        // Re-apply filters in case logs loaded first
                        applyFilters();
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        // Setup UI
        tvTitle = findViewById(R.id.tvHistoryTitle);
        btnFilter = findViewById(R.id.btnFilter);
        recyclerView = findViewById(R.id.rvHistory);
        btnExport = findViewById(R.id.btnExport);
        
        if (!childName.isEmpty()) {
            tvTitle.setText("History: " + childName);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(filteredLogs);
        recyclerView.setAdapter(adapter);

        btnFilter.setOnClickListener(v -> showFilterDialog());
        btnExport.setOnClickListener(v -> showExportOptionsDialog());

        if (childId != null) {
            loadLogs();
        } else {
            Toast.makeText(this, "Error: No ID provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadLogs() {
        mDatabase.child(childId).orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allLogs.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    SymptomLog log = data.getValue(SymptomLog.class);
                    if (log != null) {
                        allLogs.add(log);
                    }
                }
                // Newest first
                Collections.reverse(allLogs);
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_filter_history, null);
        builder.setView(view);

        TextView tvMonth3 = view.findViewById(R.id.tvMonth3);
        TextView tvMonth4 = view.findViewById(R.id.tvMonth4);
        TextView tvMonth5 = view.findViewById(R.id.tvMonth5);
        TextView tvMonth6 = view.findViewById(R.id.tvMonth6);
        
        // Simple selection logic (could be RadioGroup, but custom TextViews look nicer)
        View.OnClickListener monthListener = v -> {
             // Reset all backgrounds (simplified)
             tvMonth3.setBackgroundResource(R.drawable.bg_filter_chip_unselected);
             tvMonth4.setBackgroundResource(R.drawable.bg_filter_chip_unselected);
             tvMonth5.setBackgroundResource(R.drawable.bg_filter_chip_unselected);
             tvMonth6.setBackgroundResource(R.drawable.bg_filter_chip_unselected);
             // Set selected
             v.setBackgroundResource(R.drawable.bg_filter_chip_selected);
             
             if (v == tvMonth3) filterMonths = 3;
             else if (v == tvMonth4) filterMonths = 4;
             else if (v == tvMonth5) filterMonths = 5;
             else if (v == tvMonth6) filterMonths = 6;
        };
        
        tvMonth3.setOnClickListener(monthListener);
        tvMonth4.setOnClickListener(monthListener);
        tvMonth5.setOnClickListener(monthListener);
        tvMonth6.setOnClickListener(monthListener);

        // Pre-select current month filter
        if (filterMonths == 3) tvMonth3.performClick();
        else if (filterMonths == 4) tvMonth4.performClick();
        else if (filterMonths == 5) tvMonth5.performClick();
        else if (filterMonths == 6) tvMonth6.performClick();

        // Load Checkboxes for content filter

        TextView btnSelectSymptoms = view.findViewById(R.id.btnSelectSymptoms);
        TextView btnSelectTriggers = view.findViewById(R.id.btnSelectTriggers);
        
        btnSelectSymptoms.setOnClickListener(v -> showMultiSelect("Symptoms", SYMPTOM_OPTIONS, selectedSymptoms));
        btnSelectTriggers.setOnClickListener(v -> showMultiSelect("Triggers", TRIGGER_OPTIONS, selectedTriggers));

        builder.setPositiveButton("Apply", (dialog, which) -> applyFilters());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showMultiSelect(String title, String[] options, boolean[] selectionState) {
        new AlertDialog.Builder(this)
            .setTitle("Select " + title)
            .setMultiChoiceItems(options, selectionState, (dialog, which, isChecked) -> {
                selectionState[which] = isChecked;
            })
            .setPositiveButton("OK", null)
            .show();
    }

    private void applyFilters() {
        filteredLogs.clear();
        long cutoff = System.currentTimeMillis() - (filterMonths * 30L * 24 * 60 * 60 * 1000);

        for (SymptomLog log : allLogs) {
            // 0. Role Filter (Child sees only their own)
            if (isChildViewingOwnLogs) {
                if (log.loggedBy == null || !log.loggedBy.startsWith("Child")) {
                    continue;
                }
            }

            // 1. Time Filter
            if (log.timestamp < cutoff) continue;

            // 2. Content Filter (Symptoms)
            boolean symptomMatch = isAnySelected(selectedSymptoms); // If none selected, ignore filter (match all)
            if (symptomMatch) {
                boolean found = false;
                if (log.symptoms != null) {
                    for (String s : log.symptoms) {
                        if (isOptionSelected(s, SYMPTOM_OPTIONS, selectedSymptoms)) {
                            found = true; 
                            break;
                        }
                    }
                }
                if (!found) continue;
            }

            // 3. Content Filter (Triggers)
            boolean triggerMatch = isAnySelected(selectedTriggers);
            if (triggerMatch) {
                boolean found = false;
                if (log.triggers != null) {
                    for (String t : log.triggers) {
                        if (isOptionSelected(t, TRIGGER_OPTIONS, selectedTriggers)) {
                            found = true; 
                            break;
                        }
                    }
                }
                if (!found) continue;
            }

            filteredLogs.add(log);
        }
        adapter.notifyDataSetChanged();
    }

    private boolean isAnySelected(boolean[] selection) {
        for (boolean b : selection) if (b) return true;
        return false;
    }

    private boolean isOptionSelected(String value, String[] options, boolean[] selection) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(value) && selection[i]) return true;
        }
        return false;
    }

    private void showExportOptionsDialog() {
        if (filteredLogs.isEmpty()) {
            Toast.makeText(this, "No logs available to export", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Export as PDF", "Export as CSV"};
        new AlertDialog.Builder(this)
            .setTitle("Export History")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    generatePdf();
                } else {
                    generateCsv();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void generatePdf() {
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        int margin = 40;
        int y = 50;
        int pageHeight = 842;

        // Title
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("Symptom & Trigger Log History", margin, y, paint);
        y += 25;
        
        paint.setTextSize(14f);
        paint.setFakeBoldText(false);
        if (!childName.isEmpty()) {
            canvas.drawText("Child: " + childName, margin, y, paint);
            y += 20;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.getDefault());
        canvas.drawText("Export Date: " + sdf.format(new Date()), margin, y, paint);
        y += 40;

        paint.setTextSize(12f);
        
        for (SymptomLog log : filteredLogs) {
            // Check page bounds
            if (y > pageHeight - 100) {
                doc.finishPage(page);
                page = doc.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            // Draw Log Entry
            String dateStr = sdf.format(new Date(log.timestamp));
            String author = (log.loggedBy != null && !log.loggedBy.isEmpty()) ? log.loggedBy : "Unknown";
            
            paint.setFakeBoldText(true);
            canvas.drawText("Date: " + dateStr + "  |  Logged By: " + author, margin, y, paint);
            y += 15;

            paint.setFakeBoldText(false);
            canvas.drawText("Severity: " + log.severity + "/10", margin, y, paint);
            y += 15;

            if (log.symptoms != null && !log.symptoms.isEmpty()) {
                canvas.drawText("Symptoms: " + String.join(", ", log.symptoms), margin, y, paint);
                y += 15;
            }
            
            if (log.triggers != null && !log.triggers.isEmpty()) {
                canvas.drawText("Triggers: " + String.join(", ", log.triggers), margin, y, paint);
                y += 15;
            }

            if (log.notes != null && !log.notes.isEmpty()) {
                canvas.drawText("Notes: " + log.notes, margin, y, paint);
                y += 15;
            }

            // Divider
            Paint linePaint = new Paint();
            linePaint.setColor(Color.LTGRAY);
            linePaint.setStrokeWidth(1f);
            y += 10;
            canvas.drawLine(margin, y, 595 - margin, y, linePaint);
            y += 30;
        }

        doc.finishPage(page);

        String fileName = "logs_export_" + System.currentTimeMillis() + ".pdf";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try {
            doc.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Exported: " + fileName, Toast.LENGTH_LONG).show();
            openFile(file, "application/pdf");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting PDF", Toast.LENGTH_SHORT).show();
        } finally {
            doc.close();
        }
    }

    private void generateCsv() {
        String fileName = "logs_export_" + System.currentTimeMillis() + ".csv";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try (FileWriter writer = new FileWriter(file)) {
            writer.append("Timestamp,Date,Time,LoggedBy,Severity,Symptoms,Triggers,Notes\n");

            SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

            for (SymptomLog log : filteredLogs) {
                Date date = new Date(log.timestamp);
                
                String symptoms = (log.symptoms != null) ? String.join(";", log.symptoms) : "";
                String triggers = (log.triggers != null) ? String.join(";", log.triggers) : "";
                String notes = (log.notes != null) ? log.notes.replace("\"", "\"\"").replace("\n", " ") : "";
                String loggedBy = (log.loggedBy != null) ? log.loggedBy : "Unknown";

                writer.append(String.valueOf(log.timestamp)).append(",")
                      .append(dateSdf.format(date)).append(",")
                      .append(timeSdf.format(date)).append(",")
                      .append("\"").append(loggedBy).append("\",")
                      .append(String.valueOf(log.severity)).append(",")
                      .append("\"").append(symptoms).append("\",")
                      .append("\"").append(triggers).append("\",")
                      .append("\"").append(notes).append("\"\n");
            }

            Toast.makeText(this, "CSV Exported: " + fileName, Toast.LENGTH_LONG).show();
            openFile(file, "text/csv");

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting CSV", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile(File file, String mimeType) {
        Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file.", Toast.LENGTH_SHORT).show();
        }
    }
}