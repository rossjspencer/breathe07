package com.example.smartair;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class ManageChildActivity extends AppCompatActivity {

    private EditText etFirst, etLast, etDob, etUser, etPass, etNotes;
    private EditText etPlannedDoses, etPlannedDays;
    private Button btnSave, btnDelete;
    private DatabaseReference mDatabase;
    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_child);

        childId = getIntent().getStringExtra("CHILD_ID");
        if (childId == null) {
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Bind Views
        etFirst = findViewById(R.id.edit_firstname);
        etLast = findViewById(R.id.edit_lastname);
        etDob = findViewById(R.id.edit_dob);
        etUser = findViewById(R.id.edit_username);
        etPass = findViewById(R.id.edit_password);
        etNotes = findViewById(R.id.edit_notes);
        etPlannedDoses = findViewById(R.id.edit_planned_doses);
        etPlannedDays = findViewById(R.id.edit_planned_days);
        btnSave = findViewById(R.id.btn_save_changes);
        btnDelete = findViewById(R.id.btn_delete_child);

        loadChildData();

        btnSave.setOnClickListener(v -> saveChanges());

        // WIRE UP THE DELETE BUTTON
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void loadChildData() {
        mDatabase.child("users").child(childId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User child = snapshot.getValue(User.class);
                if (child != null) {
                    etFirst.setText(child.firstName);
                    etLast.setText(child.lastName);
                    etUser.setText(child.email);
                    etPass.setText(child.password);
                    if (child.dateOfBirth != null) etDob.setText(child.dateOfBirth);
                    if (child.notes != null) etNotes.setText(child.notes);
                    etPlannedDoses.setText(String.valueOf(child.plannedControllerPerDay));
                    etPlannedDays.setText(String.valueOf(child.plannedControllerDaysPerWeek));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveChanges() {
        String first = etFirst.getText().toString().trim();
        String last = etLast.getText().toString().trim();

        // Simple Update
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", first);
        updates.put("lastName", last);
        updates.put("email", etUser.getText().toString().trim());
        updates.put("password", etPass.getText().toString().trim());
        updates.put("dateOfBirth", etDob.getText().toString().trim());
        updates.put("notes", etNotes.getText().toString().trim());
        updates.put("plannedControllerPerDay", parseOrDefault(etPlannedDoses.getText().toString().trim(), 1));
        updates.put("plannedControllerDaysPerWeek", parseOrDefault(etPlannedDays.getText().toString().trim(), 7));

        mDatabase.child("users").child(childId).updateChildren(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private int parseOrDefault(String raw, int fallback) {
        try {
            int val = Integer.parseInt(raw);
            return val > 0 ? val : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // --- Delete Child from Existence ---
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Child?")
                .setMessage("This will permanently remove this child profile.")
                .setPositiveButton("Delete", (dialog, which) -> performDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete() {
        String parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Delete Data
        mDatabase.child("users").child(childId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Delete Link
                    mDatabase.child("users").child(parentId).child("linkedChildren").child(childId).removeValue()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Child Deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                });
    }
}
