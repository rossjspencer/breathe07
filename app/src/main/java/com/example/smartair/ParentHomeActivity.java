package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;

public class ParentHomeActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private String currentParentId;

    // List Components
    private RecyclerView recyclerView;
    private ChildAdapter adapter;
    private ArrayList<User> childList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_home);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Safety check: if parent isn't logged in, don't crash
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentParentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // 1. Setup List
        recyclerView = findViewById(R.id.rvLinkedChildren);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        childList = new ArrayList<>();
        adapter = new ChildAdapter(childList);
        recyclerView.setAdapter(adapter);

        // 2. Setup Button (Now Redirects to AddChildActivity)
        Button btnLink = findViewById(R.id.btnLinkChild);

        // Optional: Update text programmatically to reflect new behavior
        btnLink.setText("+ Register New Child");

        btnLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // OLD WAY: showLinkChildDialog();
                // NEW WAY: Open the registration screen
                startActivity(new Intent(ParentHomeActivity.this, AddChildActivity.class));
            }
        });

        // Setup Log Symptoms Button
        Button logButton = findViewById(R.id.log_symptoms_button);
        logButton.setText("Daily Triggers/Symptoms Log"); // Rename
        logButton.setOnClickListener(v -> handleLogButtonClick());

        // 3. Load Data
        if (currentParentId != null) {
            loadLinkedChildren();
        }
    }

    private void handleLogButtonClick() {
        if (childList.isEmpty()) {
            Toast.makeText(this, "No linked children found.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (childList.size() == 1) {
            // Only one child, launch directly
            launchLogForChild(childList.get(0));
        } else {
            // Multiple children, show selection dialog
            showChildSelectionDialog();
        }
    }

    private void showChildSelectionDialog() {
        String[] childNames = new String[childList.size()];
        for (int i = 0; i < childList.size(); i++) {
            User child = childList.get(i);
            childNames[i] = child.firstName + " " + (child.lastName != null ? child.lastName : "");
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Child to Log For")
                .setItems(childNames, (dialog, which) -> {
                    launchLogForChild(childList.get(which));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void launchLogForChild(User child) {
        Intent intent = new Intent(ParentHomeActivity.this, DailyLogActivity.class);
        intent.putExtra("CHILD_ID", child.userId);
        intent.putExtra("LOGGED_BY_ROLE", "Parent");
        startActivity(intent);
    }

    private void loadLinkedChildren() {
        mDatabase.child("users").child(currentParentId).child("linkedChildren")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        childList.clear(); // Clear list to prevent duplicates
                        for (DataSnapshot data : snapshot.getChildren()) {
                            String childUid = data.getKey();
                            fetchChildDetails(childUid);
                        }
                        // If no children, notify adapter to clear view
                        if (!snapshot.exists()) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // REPLACE THE OLD fetchChildDetails WITH THIS ONE
    private void fetchChildDetails(String childUid) {
        // Changed from addListenerForSingleValueEvent to addValueEventListener
        mDatabase.child("users").child(childUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User updatedChild = snapshot.getValue(User.class);
                if (updatedChild != null) {
                    updatedChild.userId = childUid;

                    // LOGIC: Check if child is already in the list
                    int index = -1;
                    for (int i = 0; i < childList.size(); i++) {
                        if (childList.get(i).userId.equals(childUid)) {
                            index = i;
                            break;
                        }
                    }

                    if (index != -1) {
                        // Child exists -> Update their data
                        childList.set(index, updatedChild);
                        adapter.notifyItemChanged(index);
                    } else {
                        // New child -> Add to list
                        childList.add(updatedChild);
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}