package com.example.smartair;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
        currentParentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 1. Setup List
        recyclerView = findViewById(R.id.rvLinkedChildren);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        childList = new ArrayList<>();
        adapter = new ChildAdapter(childList);
        recyclerView.setAdapter(adapter);

        // 2. Setup Button
        Button btnLink = findViewById(R.id.btnLinkChild);
        btnLink.setOnClickListener(v -> showLinkChildDialog());

        // 3. Load Data
        loadLinkedChildren();
    }

    private void loadLinkedChildren() {
        mDatabase.child("users").child(currentParentId).child("linkedChildren")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        childList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            String childUid = data.getKey();
                            fetchChildDetails(childUid);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchChildDetails(String childUid) {
        mDatabase.child("users").child(childUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User child = snapshot.getValue(User.class);
                if (child != null) {
                    // Since ID isn't inside the object by default, we can set it manually if needed
                    child.userId = childUid;
                    childList.add(child);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showLinkChildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Link Child Account");
        builder.setMessage("Enter the 6-character code from the Child's app:");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Link", (dialog, which) -> {
            String code = input.getText().toString().trim().toUpperCase();
            linkChildAccount(code);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void linkChildAccount(String inputCode) {
        if (inputCode == null || inputCode.length() < 6) {
            Toast.makeText(this, "Invalid code format", Toast.LENGTH_SHORT).show();
            return;
        }

        Query query = mDatabase.child("users").orderByChild("pairingCode").equalTo(inputCode);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        String childUid = childSnapshot.getKey();
                        if (childUid.equals(currentParentId)) {
                            Toast.makeText(ParentHomeActivity.this, "Cannot link to yourself", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Save link
                        mDatabase.child("users").child(currentParentId)
                                .child("linkedChildren").child(childUid).setValue(true)
                                .addOnSuccessListener(aVoid -> Toast.makeText(ParentHomeActivity.this, "Child Linked!", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Toast.makeText(ParentHomeActivity.this, "No child found with that code.", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ParentHomeActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
