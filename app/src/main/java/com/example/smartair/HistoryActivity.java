package com.example.smartair;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private TextView tvTitle;
    private ImageButton btnFilter;
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

        // Setup UI
        tvTitle = findViewById(R.id.tvHistoryTitle);
        btnFilter = findViewById(R.id.btnFilter);
        recyclerView = findViewById(R.id.rvHistory);
        
        if (!childName.isEmpty()) {
            tvTitle.setText("History: " + childName);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(filteredLogs);
        recyclerView.setAdapter(adapter);

        btnFilter.setOnClickListener(v -> showFilterDialog());

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
        // Note: In a real app, we'd dynamically generate checkboxes or use a multi-select dialog.
        // For simplicity here, we will just filter by time first. Content filtering 
        // requires a more complex UI (RecyclerView inside Dialog).
        // Let's just launch a standard multi-choice dialog for Symptoms/Triggers on top?
        // Or keep it simple: Just Timeframe for now as per "Polished Plan" Step 3 mentions checkboxes.
        
        // Let's use the "Content" button inside this dialog to open the multi-selects
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
}