package com.example.smartair.R4.pef;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartair.R;
import com.example.smartair.R4.logic.ZoneCalculator;
import com.example.smartair.R4.model.AsthmaZone;
import com.example.smartair.R4.model.PefEntry;
import com.example.smartair.R4.model.ZoneChangeEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PefEntryActivity extends AppCompatActivity {

    private EditText pefEditText;
    private RadioGroup prePostGroup;
    private Button saveButton;

    private final ZoneCalculator zoneCalculator = new ZoneCalculator();

    private DatabaseReference userRef;   // users/{childId}
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pef_entry);

        pefEditText  = findViewById(R.id.pef_edittext);
        prePostGroup = findViewById(R.id.pre_post_group);
        saveButton   = findViewById(R.id.save_pef_button);

        childId = getIntent().getStringExtra("childId");
        if (childId == null || childId.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "You must be logged in.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            childId = user.getUid();
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(childId);

        saveButton.setOnClickListener(v -> savePefEntry());
    }

    private void savePefEntry() {
        final String pefText = pefEditText.getText().toString().trim();
        if (pefText.isEmpty()) {
            pefEditText.setError("PEF value required");
            pefEditText.requestFocus();
            return;
        }

        final int pefValue;
        try {
            pefValue = Integer.parseInt(pefText);
        } catch (NumberFormatException e) {
            pefEditText.setError("Enter a valid number");
            pefEditText.requestFocus();
            return;
        }

        final boolean preMed = prePostGroup.getCheckedRadioButtonId() == R.id.radio_pre;

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                // PB must already be set by the Parent (per spec)
                Integer personalBest = snap.child("personalBest").getValue(Integer.class);
                if (personalBest == null || personalBest <= 0) {
                    Toast.makeText(PefEntryActivity.this,
                            "Personal Best (PB) not set. Ask your parent to set PB first.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // Compute zone using PB
                String previousZoneStr = snap.child("currentZone").getValue(String.class);
                AsthmaZone newZone = zoneCalculator.computeZone(pefValue, personalBest);
                String newZoneStr = newZone.name();
                long now = System.currentTimeMillis();

                // Log PEF entry
                DatabaseReference pefRef = userRef.child("pef_entries").push();
                String pefId = pefRef.getKey();
                PefEntry entry = new PefEntry(pefId, childId, now, pefValue, preMed);
                pefRef.setValue(entry);

                // Update current zone
                userRef.child("currentZone").setValue(newZoneStr);

                // Zone-change history if changed
                if (previousZoneStr == null || !previousZoneStr.equals(newZoneStr)) {
                    DatabaseReference zRef = userRef.child("zone_changes").push();
                    String zoneId = zRef.getKey();
                    ZoneChangeEntry z = new ZoneChangeEntry(
                            zoneId,
                            childId,
                            now,
                            previousZoneStr == null ? "UNKNOWN" : previousZoneStr,
                            newZoneStr,
                            pefValue
                    );
                    zRef.setValue(z);
                }

                // Update percent for dashboard
                int percent = (int) Math.round((pefValue * 100.0) / personalBest);
                if (percent < 0) percent = 0;
                if (percent > 100) percent = 100;
                userRef.child("asthmaScore").setValue(percent);

                Toast.makeText(PefEntryActivity.this,
                        "Saved. Zone: " + newZoneStr + " (PB set by parent: " + personalBest + ")",
                        Toast.LENGTH_LONG).show();

                finish();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PefEntryActivity.this,
                        "Database error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
