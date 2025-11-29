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
import com.example.smartair.R4.model.*;
import com.example.smartair.R4.notify.ParentAlertWriter;
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
    private String parentUid, childId;
    private CountDownTimer timer;
    private TriageIncident lastIncident;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_triage);

        root = FirebaseDatabase.getInstance().getReference();
        parentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        childId = parentUid; // TODO: replace with real childId when R2 is done

        cbCantSpeak = findViewById(R.id.cb_cant_speak);
        cbRetractions = findViewById(R.id.cb_retractions);
        cbBlueLips = findViewById(R.id.cb_blue_lips);
        etOptionalPEF = findViewById(R.id.et_optional_pef);
        npRescueCount = findViewById(R.id.np_rescue_count);
        npRescueCount.setMinValue(0); npRescueCount.setMaxValue(10);

        btnDecide = findViewById(R.id.btn_decide);
        btnHomeSteps = findViewById(R.id.btn_home_steps);
        btnCallEmergency = findViewById(R.id.btn_call_emergency);

        btnDecide.setOnClickListener(v -> runDecision());
        btnHomeSteps.setOnClickListener(v -> {
            showHomeStepsDialog();
            startHomeStepsTimer();
        });
        btnCallEmergency.setOnClickListener(v -> escalate("User chose to call emergency"));
    }

    private void runDecision() {
        boolean anyRed = cbCantSpeak.isChecked() || cbRetractions.isChecked() || cbBlueLips.isChecked();

        Integer pefValue = null;
        try {
            String t = etOptionalPEF.getText().toString().trim();
            if (!t.isEmpty()) {
                pefValue = Integer.parseInt(t);
            }
        } catch (NumberFormatException ignored) {}

        final Integer finalPef = pefValue;

        int rescueAttempts = npRescueCount.getValue();
        final int finalRescueAttempts = rescueAttempts;

        DatabaseReference childRef = root.child("children").child(childId);

        childRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                Integer pb = snap.child("personalBestPef").getValue(Integer.class);

                AsthmaZone z = AsthmaZone.UNKNOWN;
                if (finalPef != null && pb != null && pb > 0) {
                    z = zoneCalc.computeZone(finalPef, pb);
                }

                TriageDecider.Decision d = decider.decide(anyRed, z, finalRescueAttempts);
                showDecision(d);
                saveIncident(d, anyRed, finalRescueAttempts, finalPef);

                ParentAlertWriter.notify(root, parentUid, childId,
                        "TRIAGE_START", "Triage started: " + d.name());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }


    private void showDecision(TriageDecider.Decision d) {
        btnHomeSteps.setEnabled(d != TriageDecider.Decision.EMERGENCY);
        btnCallEmergency.setEnabled(true);
        Toast.makeText(this,
                (d == TriageDecider.Decision.EMERGENCY) ? "Call Emergency Now" :
                        (d == TriageDecider.Decision.HOME_STEPS) ? "Start Home Steps" : "Monitor",
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
            @Override public void onTick(long ms) { /* optional: update UI */ }
            @Override public void onFinish() {
                escalate("Auto escalation after 10 minutes without improvement");
            }
        };
        timer.start();
        Toast.makeText(this, "Home steps started. Re-check in 10 minutes.", Toast.LENGTH_LONG).show();
    }

    private void showHomeStepsDialog() {
        String msg =
                "• Stay calm and take slow breaths.\n" +
                        "• Use rescue medication if prescribed.\n" +
                        "• Sit upright and avoid triggers.\n" +
                        "• Keep monitoring symptoms.\n\n" +
                        "The app will re-check in 10 minutes.";

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Home Steps")
                .setMessage(msg)
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .create();

        if (!isFinishing()) dlg.show();
    }


    private void escalate(String reason) {
        if (lastIncident != null) {
            lastIncident.escalated = true;
            root.child("children_triage_incidents").child(childId).child(lastIncident.id).setValue(lastIncident);
        }
        ParentAlertWriter.notify(root, parentUid, childId, "TRIAGE_ESCALATION", reason);
        Toast.makeText(this, "Escalated. Consider calling emergency.", Toast.LENGTH_LONG).show();
    }

    @Override protected void onDestroy() { if (timer != null) timer.cancel(); super.onDestroy(); }
}
