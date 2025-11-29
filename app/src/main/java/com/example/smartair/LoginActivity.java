package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

public class LoginActivity extends AppCompatActivity {

    private EditText emailTextView, passwordTextView;
    private Button button;
    private FirebaseAuth auth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        emailTextView = findViewById(R.id.email_edittext);
        passwordTextView = findViewById(R.id.password_edittext);
        button = findViewById(R.id.login_button);

        button.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        // Clean input
        String input = emailTextView.getText().toString().trim();
        String password = passwordTextView.getText().toString().trim();

        // verify we have input
        if (input.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter credentials", Toast.LENGTH_LONG).show();
            return;
        }

        // Decide which login process to take based on input
        if (input.contains("@")) {
            loginParentOrProvider(input, password);
        } else {
            loginChild(input, password);
        }
    }

    // Parent and Provider login. Needed because child has no email.

    private void loginParentOrProvider(String email, String password) {
        // Sends info to firebase AND waits. When request is done, following code is executed
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String uid = task.getResult().getUser().getUid();
                            checkRoleAndRedirect(uid);
                        } else {
                            Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Child login
    private void loginChild(String username, String password) {
        // We saved the username in the 'email' field of the Database Object in AddChildActivity
        mDatabase.orderByChild("email").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Username exists, now check password
                            boolean matchFound = false;
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                User user = userSnap.getValue(User.class);

                                // Verify Password
                                if (user != null && user.password != null && user.password.equals(password)) {
                                    matchFound = true;
                                    String childId = userSnap.getKey();

                                    // SUCCESS!
                                    Toast.makeText(LoginActivity.this, "Welcome back, " + user.firstName, Toast.LENGTH_SHORT).show();

                                    // Pass the Child's ID to the Home Screen
                                    Intent intent = new Intent(LoginActivity.this, ChildHomeActivity.class);
                                    intent.putExtra("CHILD_ID", childId);
                                    startActivity(intent);
                                    finish();
                                    return;
                                }
                            }
                            if (!matchFound) {
                                Toast.makeText(LoginActivity.this, "Incorrect Password", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Username not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper to redirect Parents/Providers
    private void checkRoleAndRedirect(String uid) {
        mDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    if (role != null) {
                        if (role.equals("Parent")) {
                            startActivity(new Intent(LoginActivity.this, ParentHomeActivity.class));
                            finish();
                        } else if (role.equals("Provider")) {
                            startActivity(new Intent(LoginActivity.this, ProviderHomeActivity.class));
                            finish();
                        } else {
                            // Fallback for safety
                            Toast.makeText(LoginActivity.this, "Role not supported: " + role, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}