package com.example.smartair;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;

public class DailyLogActivity extends AppCompatActivity {

    private SeekBar severitySeekBar;
    private TextView severityValueText;
    private EditText notesInput;
    private Button saveButton;
    // Symptoms CheckBoxes
    private CheckBox cbCough, cbWheezing, cbShortness, cbChest;
    // Triggers CheckBoxes
    private CheckBox cbDust, cbPollen, cbSmoke, cbExercise;
    private DatabaseReference databaseRef;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_log);
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("logs");
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        severitySeekBar = findViewById(R.id.severity_seekbar);
        severityValueText = findViewById(R.id.severity_value_text);
        notesInput = findViewById(R.id.notes_input);
        saveButton = findViewById(R.id.save_log_button);
        // Symptoms
        cbCough = findViewById(R.id.cb_cough);
        cbWheezing = findViewById(R.id.cb_wheezing);
        cbShortness = findViewById(R.id.cb_shortness);
        cbChest = findViewById(R.id.cb_chest);
        // Triggers
        cbDust = findViewById(R.id.cb_dust);
        cbPollen = findViewById(R.id.cb_pollen);
        cbSmoke = findViewById(R.id.cb_smoke);
        cbExercise = findViewById(R.id.cb_exercise);
    }

    private void setupListeners() {
        severitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                severityValueText.setText("Level: " + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        saveButton.setOnClickListener(v -> saveLog());
    }

    private void saveLog() {
        String uid = auth.getCurrentUser().getUid();
        if (uid == null) return;
        List<String> symptoms = new ArrayList<>();
        if (cbCough.isChecked()) symptoms.add("Cough");
        if (cbWheezing.isChecked()) symptoms.add("Wheezing");
        if (cbShortness.isChecked()) symptoms.add("Shortness of Breath");
        if (cbChest.isChecked()) symptoms.add("Chest Tightness");

        List<String> triggers = new ArrayList<>();
        if (cbDust.isChecked()) triggers.add("Dust");
        if (cbPollen.isChecked()) triggers.add("Pollen");
        if (cbSmoke.isChecked()) triggers.add("Smoke");
        if (cbExercise.isChecked()) triggers.add("Exercise");

        int severity = severitySeekBar.getProgress();
        String notes = notesInput.getText().toString();
        long timestamp = System.currentTimeMillis();
        SymptomLog log = new SymptomLog(timestamp, symptoms, triggers, severity, notes);
        String logId = databaseRef.child(uid).push().getKey();

        if (logId != null) {
            databaseRef.child(uid).child(logId).setValue(log)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(DailyLogActivity.this, "Log saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(DailyLogActivity.this, "Failed to save log.", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }
}
