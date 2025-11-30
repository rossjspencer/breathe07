package com.example.smartair;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
    private TextView tvLogChildName;
    private EditText notesInput;
    private Button saveButton;
    private Button cancelButton;

    // CheckBoxes
    private CheckBox cbCough, cbWheezing, cbShortness, cbChest, cbNightWaking, cbDust, cbPollen,
            cbSmoke, cbExercise, cbPets, cbColdAir, cbIllness, cbPcs;
    private RadioGroup rgActivityLimitation;
    private DatabaseReference databaseRef;
    private FirebaseAuth auth;

    // New Variables for Targeting
    private String targetChildId = null;
    private String loggedByRole = "Parent"; // Default
    private String childName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_log);
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("logs");

        // Extract Extras
        if (getIntent().hasExtra("CHILD_ID")) {
            targetChildId = getIntent().getStringExtra("CHILD_ID");
        }
        if (getIntent().hasExtra("LOGGED_BY_ROLE")) {
            loggedByRole = getIntent().getStringExtra("LOGGED_BY_ROLE");
        }
        if (getIntent().hasExtra("CHILD_NAME")) {
            childName = getIntent().getStringExtra("CHILD_NAME");
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        severitySeekBar = findViewById(R.id.severity_seekbar);
        severityValueText = findViewById(R.id.severity_value_text);
        tvLogChildName = findViewById(R.id.tvLogChildName);
        notesInput = findViewById(R.id.notes_input);
        saveButton = findViewById(R.id.save_log_button);
        cancelButton = findViewById(R.id.cancel_log_button);

        // Symptoms
        cbCough = findViewById(R.id.cb_cough);
        cbWheezing = findViewById(R.id.cb_wheezing);
        cbShortness = findViewById(R.id.cb_shortness);
        cbChest = findViewById(R.id.cb_chest);
        cbNightWaking = findViewById(R.id.cb_night_waking);

        rgActivityLimitation = findViewById(R.id.rg_activity_limitation);
        // Triggers
        cbDust = findViewById(R.id.cb_dust);
        cbPollen = findViewById(R.id.cb_pollen);
        cbSmoke = findViewById(R.id.cb_smoke);
        cbExercise = findViewById(R.id.cb_exercise);
        cbPets = findViewById(R.id.cb_pets);
        cbColdAir = findViewById(R.id.cb_cold_air);
        cbIllness = findViewById(R.id.cb_illness);
        cbPcs = findViewById(R.id.cb_pcs);

        // Set Child Name if available
        if (childName != null && !childName.isEmpty()) {
            tvLogChildName.setText("(" + childName + ")");
        } else {
            tvLogChildName.setText("");
        }
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
        cancelButton.setOnClickListener(v -> finish());
    }

    private void saveLog() {
        String uid;
        if (targetChildId != null) {
            uid = targetChildId;
        } else {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
                return;
            }
            uid = auth.getCurrentUser().getUid();
        }

        List<String> symptoms = new ArrayList<>();
        if (cbCough.isChecked()) symptoms.add("Cough");
        if (cbWheezing.isChecked()) symptoms.add("Wheezing");
        if (cbShortness.isChecked()) symptoms.add("Shortness of Breath");
        if (cbChest.isChecked()) symptoms.add("Chest Tightness");
        if (cbNightWaking.isChecked()) symptoms.add("Night Waking");

        List<String> triggers = new ArrayList<>();
        if (cbDust.isChecked()) triggers.add("Dust");
        if (cbPollen.isChecked()) triggers.add("Pollen");
        if (cbSmoke.isChecked()) triggers.add("Smoke");
        if (cbExercise.isChecked()) triggers.add("Exercise");
        if (cbPets.isChecked()) triggers.add("Pets");
        if (cbColdAir.isChecked()) triggers.add("Cold air");
        if (cbIllness.isChecked()) triggers.add("Illness");
        if (cbPcs.isChecked()) triggers.add("Perfumes/Cleaners/Strong Odors");

        String activityLimitation = "None";
        int selectedId = rgActivityLimitation.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            activityLimitation = selectedRadioButton.getText().toString();
        }

        int severity = severitySeekBar.getProgress();
        String notes = notesInput.getText().toString();
        long timestamp = System.currentTimeMillis();

        // Determine final "Logged By" string
        String loggedByString;
        if ("Child".equals(loggedByRole)) {
            loggedByString = "Child - " + (childName.isEmpty() ? "Unknown" : childName);
        } else {
            loggedByString = "Parent";
        }

        SymptomLog log = new SymptomLog(timestamp, symptoms, triggers, severity, activityLimitation, notes, loggedByString);

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