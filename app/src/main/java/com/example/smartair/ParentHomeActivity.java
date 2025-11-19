//package com.example.smartair;
//
//import android.os.Bundle;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.*;
//
//
//public class ParentHomeActivity extends AppCompatActivity {
//
//    private DatabaseReference mDatabase;
//    private String currentParentId;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        TextView tv = new TextView(this);
//        tv.setTextSize(24);
//        tv.setText("Welcome Parent!");
//        tv.setPadding(50, 50, 50, 50);
//
//        setContentView(tv);
//        mDatabase = FirebaseDatabase.getInstance().getReference();
//        currentParentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//    }
//
//    private void linkChildAccount(String inputCode) {
//        // Critical check: valid input
//        if (inputCode == null || inputCode.length() < 6) {
//            Toast.makeText(this, "Invalid code format", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        Query query = mDatabase.child("users").orderByChild("pairingCode").equalTo(inputCode);
//        query.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    // We found the child!
//                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
//                        String childUid = childSnapshot.getKey();
//
//                        // CRITICAL: Prevent self-linking or double-linking
//                        if (childUid.equals(currentParentId)) {
//                            Toast.makeText(ParentHomeActivity.this, "Cannot link to yourself", Toast.LENGTH_SHORT).show();
//                            return;
//                        }
//
//                        // Perform the Link: Add childUID to parent's linkedChildren map
//                        mDatabase.child("users").child(currentParentId)
//                                .child("linkedChildren").child(childUid).setValue(true)
//                                .addOnSuccessListener(aVoid -> {
//                                    Toast.makeText(ParentHomeActivity.this, "Child Linked Successfully!", Toast.LENGTH_LONG).show();
//                                    // Optional: Refresh UI to show new child
//                                })
//                                .addOnFailureListener(e -> {
//                                    Toast.makeText(ParentHomeActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                });
//                    }
//                } else {
//                    Toast.makeText(ParentHomeActivity.this, "No child found with that code.", Toast.LENGTH_LONG).show();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(ParentHomeActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//
//}

package com.example.smartair;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class ParentHomeActivity extends AppCompatActivity {

    // ==========================================
    // 1. CLASS-LEVEL VARIABLES (Declare them here!)
    // ==========================================
    private DatabaseReference mDatabase;
    private String currentParentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_home); // Ensure this matches your XML file name

        // ==========================================
        // 2. INITIALIZE THEM (Give them values here)
        // ==========================================
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentParentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Setup the button
        Button btnLink = findViewById(R.id.btnLinkChild);
        btnLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLinkChildDialog();
            }
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
        // Critical check: valid input
        if (inputCode == null || inputCode.length() < 6) {
            Toast.makeText(this, "Invalid code format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Now 'mDatabase' works because it was declared at the top!
        Query query = mDatabase.child("users").orderByChild("pairingCode").equalTo(inputCode);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        String childUid = childSnapshot.getKey();

                        // Now 'currentParentId' works because it was declared at the top!
                        if (childUid.equals(currentParentId)) {
                            Toast.makeText(ParentHomeActivity.this, "Cannot link to yourself", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        mDatabase.child("users").child(currentParentId)
                                .child("linkedChildren").child(childUid).setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(ParentHomeActivity.this, "Child Linked Successfully!", Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ParentHomeActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
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
