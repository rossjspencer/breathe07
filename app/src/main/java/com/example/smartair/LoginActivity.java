package com.example.smartair;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Button loginBtn;
    private TextView forgotPassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        emailField = findViewById(R.id.email_edittext);
        passwordField = findViewById(R.id.password_edittext);
        loginBtn = findViewById(R.id.login_button);
        forgotPassword = findViewById(R.id.forgot_password);

        loginBtn.setOnClickListener(v -> loginUser());

        // NEW: Forgot Password auto-send
        forgotPassword.setOnClickListener(v -> sendResetEmailAutomatically());
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        // Validate inputs
        if (!AuthHelper.validateCredentials(email, password)) {
            AuthHelper.showToast(this, "Please enter all fields");
            return;
        }

        // Attempt login
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        AuthHelper.showToast(this, "Login failed");
                        return;
                    }

                    String uid = task.getResult().getUser().getUid();

                    // Read user role from database
                    AuthHelper.getUserRef(uid).addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!snapshot.exists()) {
                                        AuthHelper.showToast(LoginActivity.this, "User not found");
                                        return;
                                    }

                                    String role = snapshot.child("role").getValue(String.class);

                                    if (role == null) {
                                        AuthHelper.showToast(LoginActivity.this, "Role missing");
                                        return;
                                    }

                                    // Navigate based on role
                                    AuthHelper.redirectUser(LoginActivity.this, role);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    AuthHelper.showToast(LoginActivity.this, "Database error");
                                }
                            });
                });
    }

    private void showPasswordResetDialog() {
        EditText resetEmail = new EditText(this);
        resetEmail.setHint("Enter your email");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your registered email to receive a reset link.")
                .setView(resetEmail)
                .setPositiveButton("Send", (dialog, which) -> {

                    String email = resetEmail.getText().toString().trim();

                    if (email.isEmpty()) {
                        AuthHelper.showToast(this, "Please enter your email");
                        return;
                    }

                    // Step 1 — Check if email exists in DATABASE
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

                            // Step 2 — Email exists → send reset link
                            auth.sendPasswordResetEmail(email)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            AuthHelper.showToast(LoginActivity.this, "Reset link sent!");
                                        } else {
                                            AuthHelper.showToast(LoginActivity.this, "Failed to send reset link.");
                                        }
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            AuthHelper.showToast(LoginActivity.this, "Database error");
                        }
                    });

                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void sendResetEmailAutomatically() {
        String email = emailField.getText().toString().trim();

        if (email.isEmpty()) {
            AuthHelper.showToast(this, "Please enter your email first");
            return;
        }

        // Check if email exists in database
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

                // SEND RESET EMAIL
                auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {

                                // --- SHOW POPUP HERE ---
                                new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this)
                                        .setTitle("Reset Email Sent")
                                        .setMessage("A password reset link has been sent to:\n\n" + email)
                                        .setPositiveButton("OK", null)
                                        .create()
                                        .show();
                                // -----------------------

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

}

