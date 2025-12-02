package com.example.smartair;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AddChildActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etUsername, etPassword;
    private Button btnSave;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        etFirstName = findViewById(R.id.child_firstname);
        etLastName = findViewById(R.id.child_lastname);
        etUsername = findViewById(R.id.child_username);
        etPassword = findViewById(R.id.child_password);
        btnSave = findViewById(R.id.btn_save_child);

        btnSave.setOnClickListener(v -> registerChild());
    }

    private void registerChild() {
        String first = etFirstName.getText().toString().trim();
        String last = etLastName.getText().toString().trim();
        String user = etUsername.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (first.isEmpty() || last.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("users").orderByChild("email").equalTo(user)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Username found in DB -> It is taken
                            Toast.makeText(AddChildActivity.this, "Username '" + user + "' is already taken.", Toast.LENGTH_LONG).show();
                        } else {
                            // Username is unique -> Proceed with creation
                            performCreation(first, last, user, pass);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AddChildActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performCreation(String first, String last, String user, String pass) {
        String newChildId = mDatabase.child("users").push().getKey();
        String currentParentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (newChildId != null && currentParentId != null) {
            User newChild = new User(newChildId, "Child", user);

            newChild.firstName = first;
            newChild.lastName = last;
            newChild.password = pass;

            mDatabase.child("users").child(newChildId).setValue(newChild)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mDatabase.child("users").child(currentParentId)
                                    .child("linkedChildren").child(newChildId).setValue(true);

                            Toast.makeText(AddChildActivity.this, "Child Registered & Linked!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddChildActivity.this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}