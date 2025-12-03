package com.example.smartair;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.content.pm.PackageManager;

import com.example.smartair.r3.InventoryLogActivity;
import com.example.smartair.r3.InventoryAlertService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class ParentHomeActivity extends AppCompatActivity {
    private Button logoutBtn;
    private static final int PERMISSION_REQUEST_CODE = 112;

    // Main branch
    private DatabaseReference mDatabase;
    private String currentParentId;

    // Child List
    private RecyclerView recyclerView;
    private ChildAdapter adapter;
    private ArrayList<User> childList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_home);

        // Security: Must be logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        
        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            } else {
                // Start Inventory Alert Service if permission granted
                startService(new Intent(this, InventoryAlertService.class));
            }
        } else {
            // Start Inventory Alert Service for older android versions
            startService(new Intent(this, InventoryAlertService.class));
        }

        // Logout button (your branch)
        logoutBtn = findViewById(R.id.logout_button);
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            stopService(new Intent(this, InventoryAlertService.class)); // Stop service on logout
            startActivity(new Intent(ParentHomeActivity.this, MainActivity.class));
            finish();
        });

        // Main branch database logic
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get parent UID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentParentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Setup child list (main branch)
        recyclerView = findViewById(R.id.rvLinkedChildren);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        childList = new ArrayList<>();
        adapter = new ChildAdapter(childList);
        recyclerView.setAdapter(adapter);

        // Add child button
        Button btnLink = findViewById(R.id.btnLinkChild);
        btnLink.setText("+ Register New Child");

        btnLink.setOnClickListener(v ->
                startActivity(new Intent(ParentHomeActivity.this, AddChildActivity.class)));

        // 3. Load Data
        if (currentParentId != null) {
            loadLinkedChildren();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start service
                startService(new Intent(this, InventoryAlertService.class));
            } else {
                Toast.makeText(this, "Notifications required for alerts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Load list of child IDs linked to parent
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

                        if (!snapshot.exists()) {
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // load child details and update RecyclerView
    private void fetchChildDetails(String childUid) {
        mDatabase.child("users").child(childUid)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User updatedChild = snapshot.getValue(User.class);

                        if (updatedChild != null) {
                            updatedChild.userId = childUid;

                            int index = -1;
                            for (int i = 0; i < childList.size(); i++) {
                                if (childList.get(i).userId.equals(childUid)) {
                                    index = i;
                                    break;
                                }
                            }

                            if (index != -1) {
                                // Update existing
                                childList.set(index, updatedChild);
                                adapter.notifyItemChanged(index);
                            } else {
                                // Add new child
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