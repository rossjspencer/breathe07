package com.example.smartair;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class ChildHomeActivity extends AppCompatActivity {

    private TextView tvCurrentZone, tvZoneDescription;
    private DatabaseReference mDatabase;
    private String currentChildId;
    private int personalBest = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_home);

        // Bind Views
        tvCurrentZone = findViewById(R.id.tvCurrentZone);
        tvZoneDescription = findViewById(R.id.tvZoneDescription);
        Button btnLogPef = findViewById(R.id.btnLogPef);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (getIntent().hasExtra("CHILD_ID")) {
            currentChildId = getIntent().getStringExtra("CHILD_ID");
        }
        else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentChildId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (currentChildId != null) {
            loadChildData();
        } else {
            Toast.makeText(this, "Error: Not Logged In", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnLogPef.setOnClickListener(v -> showLogDialog());
    }

    private void loadChildData() {
        mDatabase.child("users").child(currentChildId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.hasChild("personalBest")) {
                        Integer pb = snapshot.child("personalBest").getValue(Integer.class);
                        if (pb != null && pb > 0) personalBest = pb;
                    }

                    if (snapshot.hasChild("asthmaScore")) {
                        Integer score = snapshot.child("asthmaScore").getValue(Integer.class);
                        updateZoneUI(score != null ? score : 100);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateZoneUI(int score) {
        tvCurrentZone.setText(score + "%");

        if (score >= 80) {
            tvCurrentZone.setTextColor(Color.parseColor("#4CAF50"));
            tvZoneDescription.setText("Green Zone (All Good)");
            tvZoneDescription.setTextColor(Color.parseColor("#4CAF50"));
        } else if (score >= 50) {
            tvCurrentZone.setTextColor(Color.parseColor("#FFC107"));
            tvZoneDescription.setText("Yellow Zone (Caution)");
            tvZoneDescription.setTextColor(Color.parseColor("#FFC107"));
        } else {
            tvCurrentZone.setTextColor(Color.parseColor("#F44336"));
            tvZoneDescription.setText("Red Zone (Danger)");
            tvZoneDescription.setTextColor(Color.parseColor("#F44336"));
        }
    }
    private void showLogDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Log Peak Flow");
        builder.setMessage("Enter value (e.g. 350):");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Log", (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (!val.isEmpty()) {
                try {
                    int value = Integer.parseInt(val);
                    calculateAndSaveScore(value);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid Number", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void calculateAndSaveScore(int currentPef) {
        int asthmaScore = (int) (((float) currentPef / personalBest) * 100);
        if (asthmaScore > 100) asthmaScore = 100;

        mDatabase.child("users").child(currentChildId).child("asthmaScore").setValue(asthmaScore)
                .addOnSuccessListener(v -> Toast.makeText(this, "Logged successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}