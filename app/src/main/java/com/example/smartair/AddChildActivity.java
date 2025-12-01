package com.example.smartair;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddChildActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etUsername, etPassword;
    private Button btnSave;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);

        // Establish Firebase Database reference
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etFirstName = findViewById(R.id.child_firstname);
        etLastName = findViewById(R.id.child_lastname);
        etUsername = findViewById(R.id.child_username);
        etPassword = findViewById(R.id.child_password);
        btnSave = findViewById(R.id.btn_save_child);

        btnSave.setOnClickListener(v -> registerChild());
    }

    private void registerChild() {
        // Clean inputs
        String first = etFirstName.getText().toString().trim();
        String last = etLastName.getText().toString().trim();
        String user = etUsername.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        // Check if inputs are empty
        if (first.isEmpty() || last.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate id for new child
        String newChildId = mDatabase.child("users").push().getKey();
        String currentParentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (newChildId != null && currentParentId != null) {

            // --- FIX APPLIED HERE ---
            // Use the new 3-argument constructor: ID, Role, Username (stored in email field)
            User newChild = new User(newChildId, "Child", user);

            // Set remaining fields manually
            newChild.firstName = first;
            newChild.lastName = last;
            newChild.password = pass;

            // Writes new user to database AND waits before linking
            mDatabase.child("users").child(newChildId).setValue(newChild)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Link to Parent
                            mDatabase.child("users").child(currentParentId)
                                    .child("linkedChildren").child(newChildId).setValue(true);
                            
                            // Store creation date in guide_stats
                            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                            mDatabase.getRoot().child("guide_stats").child(newChildId).child("accountCreationDate").setValue(today);

                            Toast.makeText(this, "Child Registered & Linked!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}