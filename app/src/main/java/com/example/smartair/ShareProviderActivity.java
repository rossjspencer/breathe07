package com.example.smartair;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class ShareProviderActivity extends AppCompatActivity {

    private String childId;
    private DatabaseReference mDatabase;

    // UI Elements
    private SwitchMaterial masterSwitch;
    private LinearLayout granularLayout;
    private CheckBox cbRescue, cbController, cbSymptoms, cbTriggers, cbPef, cbTriage, cbCharts;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_provider);

        childId = getIntent().getStringExtra("CHILD_ID");
        if (childId == null) { finish(); return; }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        bindViews();
        setupListeners();
        loadCurrentSettings();
    }

    private void bindViews() {
        masterSwitch = findViewById(R.id.switch_master_share);
        granularLayout = findViewById(R.id.layout_granular_options);
        btnSave = findViewById(R.id.btn_save_sharing);

        cbRescue = findViewById(R.id.check_rescue);
        cbController = findViewById(R.id.check_controller);
        cbSymptoms = findViewById(R.id.check_symptoms);
        cbTriggers = findViewById(R.id.check_triggers);
        cbPef = findViewById(R.id.check_pef);
        cbTriage = findViewById(R.id.check_triage);
        cbCharts = findViewById(R.id.check_charts);
    }

    private void setupListeners() {
        // Toggle Logic: "Gray out" items if master switch is OFF
        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setGranularOptionsEnabled(isChecked);
        });

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void setGranularOptionsEnabled(boolean enabled) {
        // Visual effect: Fade out if disabled
        granularLayout.setAlpha(enabled ? 1.0f : 0.4f);

        // Functional effect: Disable clicks
        for (int i = 0; i < granularLayout.getChildCount(); i++) {
            View child = granularLayout.getChildAt(i);
            child.setEnabled(enabled);
        }
    }

    private void loadCurrentSettings() {
        mDatabase.child("users").child(childId).child("sharingSettings")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Load Master Toggle
                            Boolean isShared = snapshot.child("isShared").getValue(Boolean.class);
                            masterSwitch.setChecked(isShared != null && isShared);

                            // Load Granular Toggles (Default to false if null)
                            setCheck(snapshot, "shareRescue", cbRescue);
                            setCheck(snapshot, "shareController", cbController);
                            setCheck(snapshot, "shareSymptoms", cbSymptoms);
                            setCheck(snapshot, "shareTriggers", cbTriggers);
                            setCheck(snapshot, "sharePEF", cbPef);
                            setCheck(snapshot, "shareTriage", cbTriage);
                            setCheck(snapshot, "shareCharts", cbCharts);

                            // Ensure visual state matches data
                            setGranularOptionsEnabled(masterSwitch.isChecked());
                        } else {
                            // Default State: OFF
                            masterSwitch.setChecked(false);
                            setGranularOptionsEnabled(false);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setCheck(DataSnapshot snap, String key, CheckBox cb) {
        Boolean val = snap.child(key).getValue(Boolean.class);
        cb.setChecked(val != null && val);
    }

    private void saveSettings() {
        Map<String, Object> settings = new HashMap<>();

        // Save Master State
        boolean isMasterOn = masterSwitch.isChecked();
        settings.put("isShared", isMasterOn);

        // Save Granular States
        // Even if "isShared" is FALSE, we still save the checkbox states
        // so they are remembered (but grayed out) next time.
        settings.put("shareRescue", cbRescue.isChecked());
        settings.put("shareController", cbController.isChecked());
        settings.put("shareSymptoms", cbSymptoms.isChecked());
        settings.put("shareTriggers", cbTriggers.isChecked());
        settings.put("sharePEF", cbPef.isChecked());
        settings.put("shareTriage", cbTriage.isChecked());
        settings.put("shareCharts", cbCharts.isChecked());

        mDatabase.child("users").child(childId).child("sharingSettings").updateChildren(settings)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Sharing Settings Saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving settings", Toast.LENGTH_SHORT).show());
    }
}