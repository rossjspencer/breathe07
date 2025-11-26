package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
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

        // 3. Load Data
        if (currentParentId != null) {
            loadLinkedChildren();
        }
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