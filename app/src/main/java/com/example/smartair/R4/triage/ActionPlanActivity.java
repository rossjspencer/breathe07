package com.example.smartair.R4.triage;

import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartair.R;
import com.google.firebase.database.*;

public class ActionPlanActivity extends AppCompatActivity {

    private EditText etGreen, etYellow, etRed, etEmergency;
    private Button btnSave;

    private DatabaseReference root;
    private String childId;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_action_plan);

        root = FirebaseDatabase.getInstance().getReference();

        childId = getIntent().getStringExtra("CHILD_ID");
        if (childId == null) {
            Toast.makeText(this, "Error: No child selected", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etGreen     = findViewById(R.id.etGreenSteps);
        etYellow    = findViewById(R.id.etYellowSteps);
        etRed       = findViewById(R.id.etRedSteps);
        etEmergency = findViewById(R.id.etEmergencySteps);
        btnSave     = findViewById(R.id.btnSavePlan);

        loadPlan();

        btnSave.setOnClickListener(v -> savePlan());
    }

    private void loadPlan() {
        DatabaseReference planRef = root.child("users").child(childId).child("actionPlan");
        planRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                etGreen.setText(s.child("greenSteps").getValue(String.class));
                etYellow.setText(s.child("yellowSteps").getValue(String.class));
                etRed.setText(s.child("redSteps").getValue(String.class));
                etEmergency.setText(s.child("emergencySteps").getValue(String.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void savePlan() {
        DatabaseReference planRef = root.child("users").child(childId).child("actionPlan");

        planRef.child("greenSteps").setValue(etGreen.getText().toString());
        planRef.child("yellowSteps").setValue(etYellow.getText().toString());
        planRef.child("redSteps").setValue(etRed.getText().toString());
        planRef.child("emergencySteps").setValue(etEmergency.getText().toString());

        Toast.makeText(this, "Action plan saved!", Toast.LENGTH_LONG).show();
        finish();
    }
}
