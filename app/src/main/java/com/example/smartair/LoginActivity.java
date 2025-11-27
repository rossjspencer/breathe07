package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Button loginBtn;
    private TextView forgotPassword;

    private FirebaseAuth auth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        emailField = findViewById(R.id.email_edittext);
        passwordField = findViewById(R.id.password_edittext);
        loginBtn = findViewById(R.id.login_button);
        forgotPassword = findViewById(R.id.forgot_password);

        // LOGIN HANDLER (combined logic)
        loginBtn.setOnClickListener(v -> handleLogin());

        // PASSWORD RESET HANDLER
        forgotPassword.setOnClickListener(v -> sendResetEmailAutomatically());
    }


    // Unified Login Logic
    private void handleLogin() {
        String input = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (input.isEmpty() || password.isEmpty()) {
            AuthHelper.showToast(this, "Please enter all fields");
            return;
        }

        // If the user typed an email → Parent or Provider login
        if (input.contains("@")) {
            loginParentOrProvider(input, password);
        }
        // Otherwise → Child username login
        else {
            loginChild(input, password);
        }
    }

    // Parent / Provider Login
    private void loginParentOrProvider(String email, String password) {

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        AuthHelper.showToast(this, "Login failed");
                        return;
                    }

                    String uid = task.getResult().getUser().getUid();
                    checkRoleAndRedirect(uid);
                });
    }


    // Child Login (username, not email)
    private void loginChild(String username, String password) {

        mDatabase.orderByChild("email").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (!snapshot.exists()) {
                            Toast.makeText(LoginActivity.this, "Username not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean matchFound = false;

                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            User user = userSnap.getValue(User.class);

                            if (user != null && user.password != null &&
                                    user.password.equals(password)) {

                                matchFound = true;
                                String childId = userSnap.getKey();

                                Toast.makeText(LoginActivity.this, "Welcome back, " + user.firstName,
                                        Toast.LENGTH_SHORT).show();

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
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LoginActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // Forgot Password Flow
    private void sendResetEmailAutomatically() {
        String email = emailField.getText().toString().trim();

        if (email.isEmpty()) {
            AuthHelper.showToast(this, "Please enter your email first");
            return;
        }

        // Check if email is registered
        AuthHelper.getUsersRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                boolean emailExists = false;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String savedEmail = userSnapshot.child("email").getValue(String.class);
                    if (email.equals(savedEmail)) {
                        emailExists = true;
                        break;
                    }
                }

                if (!emailExists) {
                    AuthHelper.showToast(LoginActivity.this, "This email is not registered");
                    return;
                }

                auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                                        .setTitle("Reset Email Sent")
                                        .setMessage("A password reset link has been sent to:\n\n" + email)
                                        .setPositiveButton("OK", null)
                                        .show();
                            } else {
                                AuthHelper.showToast(LoginActivity.this, "Failed to send reset link");
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AuthHelper.showToast(LoginActivity.this, "Database error");
            }
        });
    }


    // Role Redirect Logic
    private void checkRoleAndRedirect(String uid) {

        mDatabase.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if (!snapshot.exists()) return;

                String role = snapshot.child("role").getValue(String.class);

                if (role == null) return;

                switch (role) {
                    case "Parent":
                        startActivity(new Intent(LoginActivity.this, ParentHomeActivity.class));
                        finish();
                        break;

                    case "Provider":
                        startActivity(new Intent(LoginActivity.this, ProviderHomeActivity.class));
                        finish();
                        break;

                    default:
                        Toast.makeText(LoginActivity.this, "Unknown role: " + role,
                                Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}
