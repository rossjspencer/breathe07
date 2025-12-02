package com.example.smartair.R4.triage;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.smartair.R;
import com.example.smartair.R4.logic.ZoneCalculator;
import com.example.smartair.R4.logic.TriageDecider;
import com.example.smartair.R4.model.AsthmaZone;
import com.example.smartair.R4.model.TriageIncident;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class TriageActivity extends AppCompatActivity {

    private CheckBox cbCantSpeak, cbRetractions, cbBlueLips;
    private NumberPicker npRescueCount;
    private EditText etOptionalPEF;
    private Button btnDecide, btnHomeSteps, btnCallEmergency;

    private final ZoneCalculator zoneCalc = new ZoneCalculator();
    private final TriageDecider decider = new TriageDecider();

    private DatabaseReference root;
    private String childId;
    private CountDownTimer timer;
    private TriageIncident lastIncident;

    // zone-aware plan selection
    private AsthmaZone currentChildZoneLoadedFromDb = AsthmaZone.UNKNOWN;
    private String zoneKeyForPlan = "yellowSteps"; // default if unknown

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_triage);

        root = FirebaseDatabase.getInstance().getReference();

        // Link Child ID
        childId = getIntent().getStringExtra("CHILD_ID");
        if (childId == null) childId = getIntent().getStringExtra("childId");

        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Error: No child selected for triage", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // UI
        cbCantSpeak   = findViewById(R.id.cb_cant_speak);
        cbRetractions = findViewById(R.id.cb_retractions);
        cbBlueLips    = findViewById(R.id.cb_blue_lips);
        etOptionalPEF = findViewById(R.id.et_optional_pef);
        npRescueCount = findViewById(R.id.np_rescue_count);
        npRescueCount.setMinValue(0); npRescueCount.setMaxValue(10);

        btnDecide        = findViewById(R.id.btn_decide);
        btnHomeSteps     = findViewById(R.id.btn_home_steps);
        btnCallEmergency = findViewById(R.id.btn_call_emergency);

        btnDecide.setOnClickListener(v -> runDecision());
        btnHomeSteps.setOnClickListener(v -> { showHomeStepsDialog(); startHomeStepsTimer(); });
        btnCallEmergency.setOnClickListener(v -> escalate("User chose to call emergency"));

        // Load child's current zone once
        preloadCurrentZone();
    }

    private void preloadCurrentZone() {
        DatabaseReference usersZoneRef = root.child("users").child(childId).child("currentZone");
        usersZoneRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                boolean handled = false;
                if (s.exists()) handled = tryParseZone(s.getValue(String.class));
                if (!handled) {
                    root.child("children").child(childId).child("currentZone")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override public void onDataChange(@NonNull DataSnapshot s2) {
                                    if (s2.exists()) tryParseZone(s2.getValue(String.class));
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e2) {}
                            });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private boolean tryParseZone(String z) {
        try {
            if (z != null) { currentChildZoneLoadedFromDb = AsthmaZone.valueOf(z); return true; }
        } catch (Exception ignore) {}
        return false;
    }

    private void runDecision() {
        boolean anyRed = cbCantSpeak.isChecked() || cbRetractions.isChecked() || cbBlueLips.isChecked();

        Integer pefValue = null;
        try {
            String t = etOptionalPEF.getText().toString().trim();
            if (!t.isEmpty()) pefValue = Integer.parseInt(t);
        } catch (NumberFormatException ignored) {}

        final Integer finalPef = pefValue;
        final int finalRescueAttempts = npRescueCount.getValue();

        DatabaseReference childRef = root.child("children").child(childId);
        childRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                Integer pb = snap.child("personalBestPef").getValue(Integer.class);

                AsthmaZone z = AsthmaZone.UNKNOWN;
                if (finalPef != null && pb != null && pb > 0) z = zoneCalc.computeZone(finalPef, pb);

                TriageDecider.Decision d = decider.decide(anyRed, z, finalRescueAttempts);
                selectZoneKeyForPlan(d, z);

                showDecision(d);
                saveIncident(d, anyRed, finalRescueAttempts, finalPef);
                // No notify calls here — parent app listens to DB and shows its own local notification
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void selectZoneKeyForPlan(TriageDecider.Decision d, AsthmaZone computedZone) {
        if (d == TriageDecider.Decision.EMERGENCY) { zoneKeyForPlan = "emergencySteps"; return; }
        AsthmaZone zForPlan = (computedZone != null && computedZone != AsthmaZone.UNKNOWN)
                ? computedZone : currentChildZoneLoadedFromDb;
        switch (zForPlan) {
            case RED: zoneKeyForPlan = "redSteps"; break;
            case YELLOW: zoneKeyForPlan = "yellowSteps"; break;
            case GREEN: zoneKeyForPlan = "greenSteps"; break;
            default: zoneKeyForPlan = "yellowSteps";
        }
    }

    private void showDecision(TriageDecider.Decision d) {
        btnHomeSteps.setEnabled(d != TriageDecider.Decision.EMERGENCY);
        btnCallEmergency.setEnabled(true);
        Toast.makeText(this,
                (d == TriageDecider.Decision.EMERGENCY) ? "Call Emergency Now"
                        : (d == TriageDecider.Decision.HOME_STEPS) ? "Start Home Steps" : "Monitor",
                Toast.LENGTH_LONG).show();
    }

    private void saveIncident(TriageDecider.Decision d, boolean anyRed, int rescueAttempts, Integer pef) {
        DatabaseReference ref = root.child("children_triage_incidents").child(childId).push();
        String id = ref.getKey();

        TriageIncident inc = new TriageIncident();
        inc.id = id; inc.childId = childId;
        inc.timestampMillis = System.currentTimeMillis();
        inc.redCannotSpeak = cbCantSpeak.isChecked();
        inc.redRetractions = cbRetractions.isChecked();
        inc.redBlueLips = cbBlueLips.isChecked();
        inc.recentRescueAttempts = rescueAttempts;
        inc.pefAtIncident = pef;
        inc.decision = d.name();
        inc.userResponse = "DecisionShown";
        inc.escalated = false;

        lastIncident = inc;
        ref.setValue(inc);
    }

    private void startHomeStepsTimer() {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(10 * 60_000, 1_000) {
            @Override public void onTick(long ms) { }
            @Override public void onFinish() { escalate("Auto escalation after 10 minutes without improvement"); }
        };
        timer.start();
        Toast.makeText(this, "Home steps started. Re-check in 10 minutes.", Toast.LENGTH_LONG).show();
    }

    private void showHomeStepsDialog() {
        final String title = titleFor(zoneKeyForPlan);
        DatabaseReference planRefUsers = root.child("users").child(childId)
                .child("actionPlan").child(zoneKeyForPlan);

        planRefUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String steps = snap.exists() ? snap.getValue(String.class) : null;
                if (steps == null || steps.trim().isEmpty()) {
                    root.child("children").child(childId).child("actionPlan").child(zoneKeyForPlan)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override public void onDataChange(@NonNull DataSnapshot s2) {
                                    String s = s2.exists() ? s2.getValue(String.class) : null;
                                    showPlanDialog(title, (s == null || s.trim().isEmpty())
                                            ? defaultStepsFor(zoneKeyForPlan) : s);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e2) {
                                    showPlanDialog(title, defaultStepsFor(zoneKeyForPlan));
                                }
                            });
                } else {
                    showPlanDialog(title, steps);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                showPlanDialog(title, defaultStepsFor(zoneKeyForPlan));
            }
        });
    }

    private void showPlanDialog(String title, String steps) {
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(title).setMessage(steps)
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .create();
        if (!isFinishing()) dlg.show();
    }

    private String titleFor(String key) {
        switch (key) {
            case "greenSteps": return "Green Zone Steps";
            case "yellowSteps": return "Yellow Zone Steps";
            case "redSteps": return "Red Zone Steps";
            case "emergencySteps": return "Emergency Steps";
            default: return "Home Steps";
        }
    }

    private String defaultStepsFor(String key) {
        if ("emergencySteps".equals(key))
            return "Call emergency services now. Use rescue inhaler as directed. Do not delay.";
        if ("redSteps".equals(key))
            return "Take rescue medication now. Re-check symptoms/PEF in 10 minutes. If not improving, escalate.";
        if ("yellowSteps".equals(key))
            return "Use rescue inhaler per plan. Avoid triggers. Re-check in 10 minutes.";
        return "Continue regular medicines and monitor symptoms.";
    }

    private void escalate(String reason) {
        if (lastIncident != null) {
            lastIncident.escalated = true;
            root.child("children_triage_incidents").child(childId)
                    .child(lastIncident.id).setValue(lastIncident);
        }
        Toast.makeText(this, "Escalated. Consider calling emergency.", Toast.LENGTH_LONG).show();
    }

    @Override protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
