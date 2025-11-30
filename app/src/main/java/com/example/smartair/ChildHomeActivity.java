package com.example.smartair;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import android.content.Intent;
import com.example.smartair.R4.pef.PefEntryActivity;
import com.example.smartair.R4.triage.TriageActivity;

public class ChildHomeActivity extends AppCompatActivity {

    private TextView tvCurrentZone, tvZoneDescription;
    private DatabaseReference mDatabase;
    private String currentChildId;
    private String childName = "";
    private int personalBest = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_home);

        tvCurrentZone = findViewById(R.id.tvCurrentZone);
        tvZoneDescription = findViewById(R.id.tvZoneDescription);
        Button btnLogPef = findViewById(R.id.btnLogPef);

        Button btnLogSymptoms = findViewById(R.id.btnLogSymptoms);
        Button btnViewHistory = findViewById(R.id.btnViewHistory);

        // Triage button (keep)
        Button triageButton = findViewById(R.id.triage_button);
        triageButton.setOnClickListener(v -> {
            Intent i = new Intent(ChildHomeActivity.this, TriageActivity.class);
            i.putExtra("childId", currentChildId);
            startActivity(i);
        });

        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (getIntent().hasExtra("CHILD_ID")) {
            currentChildId = getIntent().getStringExtra("CHILD_ID");
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentChildId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (currentChildId != null) {
            loadChildData();
        } else {
            Toast.makeText(this, "Error: Not Logged In", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Launch R4 PEF screen instead of dialog
        btnLogPef.setOnClickListener(v -> {
            Intent i = new Intent(ChildHomeActivity.this, PefEntryActivity.class);
            i.putExtra("childId", currentChildId);
            startActivity(i);
        });

        // Wire up log symptoms button
        btnLogSymptoms.setOnClickListener(v -> {
            Intent intent = new Intent(ChildHomeActivity.this, DailyLogActivity.class);
            intent.putExtra("CHILD_ID", currentChildId);
            intent.putExtra("LOGGED_BY_ROLE", "Child");
            intent.putExtra("CHILD_NAME", childName);
            startActivity(intent);
        });

        // Wire up history button
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(ChildHomeActivity.this, HistoryActivity.class);
            intent.putExtra("CHILD_ID", currentChildId);
            intent.putExtra("CHILD_NAME", childName);
            startActivity(intent);
        });
    }

    private void loadChildData() {
        mDatabase.child("users").child(currentChildId).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.hasChild("firstName")) {
                        childName = snapshot.child("firstName").getValue(String.class);
                    }
                    Integer pb = snapshot.child("personalBest").getValue(Integer.class);
                    if (pb != null && pb > 0) personalBest = pb;

                    Integer score = snapshot.child("asthmaScore").getValue(Integer.class);
                    updateZoneUI(score != null ? score : 100);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
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
}
