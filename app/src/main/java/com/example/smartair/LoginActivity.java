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
        forgotPassword.setOnClickListener(v -> sendResetEmailAutomatically());
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (!AuthHelper.validateCredentials(email, password)) {
            AuthHelper.showToast(this, "Please enter all fields");
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        AuthHelper.showToast(this, "Login failed");
                        return;
                    }

                    String uid = task.getResult().getUser().getUid();
                    // same flow as MainActivity when already logged in
                    AuthHelper.handlePostAuth(this, uid);
                });
    }

    // Forgot password using email typed in the email field
    private void sendResetEmailAutomatically() {
        String email = emailField.getText().toString().trim();

        if (email.isEmpty()) {
            AuthHelper.showToast(this, "Please enter your email first");
            return;
        }

        // Check if email exists in /users
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
}



