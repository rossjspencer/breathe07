package com.example.smartair.R4.pef;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartair.R;
import com.example.smartair.R4.logic.ZoneCalculator;
import com.example.smartair.R4.model.AsthmaZone;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class ParentSetPbActivity extends AppCompatActivity {

    public static final String EXTRA_CHILD_ID = "childId";

    private EditText etPb;
    private TextView tvCurrentPb, tvInfo;
    private Button btnSave;

    private String childId;
    private DatabaseReference userRef;

    private final ZoneCalculator zoneCalculator = new ZoneCalculator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_set_pb);

        etPb = findViewById(R.id.et_pb_value);
        tvCurrentPb = findViewById(R.id.tv_current_pb);
        tvInfo = findViewById(R.id.tv_pb_info);
        btnSave = findViewById(R.id.btn_save_pb);

        childId = getIntent().getStringExtra(EXTRA_CHILD_ID);
        if (TextUtils.isEmpty(childId)) {
            Toast.makeText(this, "Missing childId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(childId);

        // Load current PB to display
        userRef.child("personalBest").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer pb = snapshot.getValue(Integer.class);
                tvCurrentPb.setText(pb != null ? String.valueOf(pb) : "—");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });

        btnSave.setOnClickListener(v -> savePb());
    }

    private void savePb() {
        String pbText = etPb.getText().toString().trim();
        if (pbText.isEmpty()) {
            etPb.setError("Enter PB (e.g., 400)");
            etPb.requestFocus();
            return;
        }

        int newPb;
        try {
            newPb = Integer.parseInt(pbText);
        } catch (NumberFormatException e) {
            etPb.setError("PB must be a number");
            etPb.requestFocus();
            return;
        }
        if (newPb <= 0) {
            etPb.setError("PB must be > 0");
            etPb.requestFocus();
            return;
        }

        // Write PB
        userRef.child("personalBest").setValue(newPb)
                .addOnSuccessListener(x -> {
                    tvCurrentPb.setText(String.valueOf(newPb));
                    tvInfo.setText("PB updated.");

                    // Recompute currentZone/asthmaScore using latest PEF entry, if any
                    Query lastPef = userRef.child("pef_entries").orderByChild("timestamp").limitToLast(1);
                    lastPef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.hasChildren()) {
                                Toast.makeText(ParentSetPbActivity.this, "Saved PB.", Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }

                            DataSnapshot last = null;
                            for (DataSnapshot ds : snapshot.getChildren()) last = ds;
                            if (last == null) {
                                Toast.makeText(ParentSetPbActivity.this, "Saved PB.", Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }

                            Integer pef = Objects.requireNonNull(last.child("pef").getValue(Integer.class));
                            // Recompute zone & percent with new PB
                            AsthmaZone zone = zoneCalculator.computeZone(pef, newPb);
                            String zoneStr = zone.name();

                            int percent = (int)Math.round((pef * 100.0) / newPb);
                            if (percent < 0) percent = 0;
                            if (percent > 100) percent = 100;

                            userRef.child("currentZone").setValue(zoneStr);
                            userRef.child("asthmaScore").setValue(percent)
                                    .addOnSuccessListener(ok -> {
                                        Toast.makeText(ParentSetPbActivity.this, "Saved PB and refreshed dashboard.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(ParentSetPbActivity.this, "PB saved, but failed to refresh score: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                        }

                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(ParentSetPbActivity.this, "PB saved. (Couldn’t refresh score: " + error.getMessage() + ")", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(ParentSetPbActivity.this, "Failed to save PB: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
