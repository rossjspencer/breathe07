package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ProviderHomeActivity extends AppCompatActivity {

    // From your branch
    private Button logoutBtn;

    // From main branch
    private DatabaseReference mDatabase;
    private String providerId;
    private RecyclerView recyclerView;
    private ProviderPatientAdapter adapter;
    private ArrayList<User> patientList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_home);

        // Security check — KEEP YOUR VERSION
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Logout button (your branch)
        logoutBtn = findViewById(R.id.logout_button);
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(ProviderHomeActivity.this, MainActivity.class));
            finish();
        });

        // Main branch logic starts here
        mDatabase = FirebaseDatabase.getInstance().getReference();
        providerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = findViewById(R.id.rvProviderPatients);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        patientList = new ArrayList<>();
        adapter = new ProviderPatientAdapter(patientList);
        recyclerView.setAdapter(adapter);

        Button btnAddPatient = findViewById(R.id.btnAddPatient);
        btnAddPatient.setOnClickListener(v -> showAddPatientDialog());

        loadPatients();
    }

    private void showAddPatientDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Patient");
        builder.setMessage("Enter the 6-digit code from the parent:");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Link", (dialog, which) -> {
            String code = input.getText().toString().trim().toUpperCase();
            redeemCode(code);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void redeemCode(String code) {
        mDatabase.child("invitations").child(code)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(ProviderHomeActivity.this, "Invalid Code", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Long expiresAt = snapshot.child("expiresAt").getValue(Long.class);

                        if (expiresAt != null && System.currentTimeMillis() > expiresAt) {
                            Toast.makeText(ProviderHomeActivity.this, "Code Expired", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Link child to provider
                        String childId = snapshot.child("childId").getValue(String.class);
                        mDatabase.child("users").child(providerId)
                                .child("linkedChildren").child(childId).setValue(true);

                        // Remove the invite (one-time use)
                        snapshot.getRef().removeValue();

                        // Clean child's activeInviteCode
                        mDatabase.child("users").child(childId).child("activeInviteCode").removeValue();

                        Toast.makeText(ProviderHomeActivity.this, "Patient Linked!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadPatients() {
        mDatabase.child("users").child(providerId).child("linkedChildren")
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        patientList.clear();

                        for (DataSnapshot data : snapshot.getChildren()) {
                            String childUid = data.getKey();
                            fetchPatientDetails(childUid);
                        }

                        if (!snapshot.exists()) adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchPatientDetails(String uid) {
        mDatabase.child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User u = snapshot.getValue(User.class);
                        if (u != null) {
                            u.userId = uid;
                            patientList.add(u);
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}


