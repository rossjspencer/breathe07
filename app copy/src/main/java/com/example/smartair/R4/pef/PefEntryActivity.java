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
import com.example.smartair.R4.model.ChildProfile;
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
    private EditText pbEditText;
    private RadioGroup prePostGroup;
    private Button saveButton;

    private ZoneCalculator zoneCalculator = new ZoneCalculator();
    private DatabaseReference rootRef;
    private String parentUid;
    private String childId;   // for now we treat "one child per parent" = parentUid

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pef_entry);

        pefEditText = findViewById(R.id.pef_edittext);
        pbEditText = findViewById(R.id.pb_edittext);
        prePostGroup = findViewById(R.id.pre_post_group);
        saveButton = findViewById(R.id.save_pef_button);

        rootRef = FirebaseDatabase.getInstance().getReference();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        parentUid = user.getUid();
        // TODO: when you implement multiple children, pass a real childId via Intent extra.
        childId = parentUid;

        saveButton.setOnClickListener(v -> savePefEntry());
    }

    private void savePefEntry() {
        String pefText = pefEditText.getText().toString().trim();
        String pbText = pbEditText.getText().toString().trim();

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

        // Reference to this child's profile
        final DatabaseReference childRef = rootRef.child("children").child(childId);

        childRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ChildProfile child = snapshot.getValue(ChildProfile.class);

                Integer personalBest = null;

                // 1) Parent manually typed a PB -> override
                if (!pbText.isEmpty()) {
                    try {
                        personalBest = Integer.parseInt(pbText);
                    } catch (NumberFormatException e) {
                        pbEditText.setError("PB must be a number");
                        pbEditText.requestFocus();
                        return;
                    }

                    // 2) No PB typed, but child already has one -> reuse existing PB
                } else if (child != null && child.getPersonalBestPef() != null) {
                    personalBest = child.getPersonalBestPef();

                    // 3) No PB stored yet at all -> treat this PEF as initial PB
                } else {
                    personalBest = pefValue;
                }

                // 4) Auto-update PB if this PEF is higher than stored PB
                if (pefValue > personalBest) {
                    personalBest = pefValue;
                }

                if (personalBest == null || personalBest <= 0) {
                    Toast.makeText(PefEntryActivity.this,
                            "Set a valid Personal Best (PB).",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                // Determine previous zone (default UNKNOWN)
                AsthmaZone previousZone =
                        (child == null) ? AsthmaZone.UNKNOWN : child.getCurrentAsthmaZone();

                // Compute new zone
                AsthmaZone newZone = zoneCalculator.computeZone(pefValue, personalBest);

                long now = System.currentTimeMillis();

                // 1) Save PEF entry
                DatabaseReference pefRef = rootRef.child("children_pef_entries")
                        .child(childId)
                        .push();
                String pefId = pefRef.getKey();

                PefEntry entry = new PefEntry(
                        pefId,
                        childId,
                        now,
                        pefValue,
                        preMed
                );
                pefRef.setValue(entry);

                // 2) Update / create child profile with PB and current zone
                if (child == null) {
                    child = new ChildProfile(
                            childId,
                            parentUid,
                            "Child",            // TODO: replace with real name when child management is added
                            personalBest,
                            newZone.name()
                    );
                } else {
                    child.setPersonalBestPef(personalBest);
                    child.setCurrentAsthmaZone(newZone);
                }

                childRef.setValue(child);

                // 3) Log zone change if zone actually changed
                if (previousZone != newZone) {
                    DatabaseReference zoneRef = rootRef.child("children_zone_history")
                            .child(childId)
                            .push();
                    String zoneId = zoneRef.getKey();

                    ZoneChangeEntry zoneEntry = new ZoneChangeEntry(
                            zoneId,
                            childId,
                            now,
                            previousZone.name(),
                            newZone.name(),
                            pefValue
                    );
                    zoneRef.setValue(zoneEntry);
                }

                Toast.makeText(PefEntryActivity.this,
                        "PEF saved. Current zone: " + newZone.name()
                                + " | PB: " + personalBest,
                        Toast.LENGTH_LONG).show();

                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PefEntryActivity.this,
                        "Database error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
