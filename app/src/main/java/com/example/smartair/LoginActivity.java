package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements LoginContract.View {

    private EditText emailField, passwordField;
    private LoginPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize MVP
        presenter = new LoginPresenter(this, new LoginModel());

        emailField = findViewById(R.id.email_edittext);
        passwordField = findViewById(R.id.password_edittext);
        Button loginBtn = findViewById(R.id.login_button);
        TextView forgotPassword = findViewById(R.id.forgot_password);

        loginBtn.setOnClickListener(v -> {
            String input = emailField.getText().toString();
            String password = passwordField.getText().toString();
            presenter.handleLogin(input, password);
        });

        // Keep forgot password logic simple or move to presenter later if needed
        forgotPassword.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();

            if (email.isEmpty()) {
                AuthHelper.showToast(this, "Please enter your email first");
                return;
            }

            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            AuthHelper.showToast(this, "A reset link has been sent to your email");
                        } else {
                            AuthHelper.showToast(this, "Failed to send reset email");
                        }
                    });
        });
    }

    @Override
    public void showInputError() {
        AuthHelper.showToast(this, "Please enter all fields");
    }

    @Override
    public void showLoginError(String message) {
        AuthHelper.showToast(this, message);
    }

    @Override
    public void showLoginSuccess(String message) {
        AuthHelper.showToast(this, message);
    }

    @Override
    public void navigateToParentHome() {
        startActivity(new Intent(this, ParentHomeActivity.class));
        finish();
    }

    @Override
    public void navigateToProviderHome() {
        startActivity(new Intent(this, ProviderHomeActivity.class));
        finish();
    }

    @Override
    public void navigateToChildHome(String childId) {
        Intent intent = new Intent(this, ChildHomeActivity.class);
        intent.putExtra("CHILD_ID", childId);
        startActivity(intent);
        finish();
    }

    @Override
    public void navigateToParentOnboarding() {
        startActivity(new Intent(this, ParentOnboardingActivity.class));
        finish();
    }

    @Override
    public void navigateToProviderOnboarding() {
        startActivity(new Intent(this, ProviderOnboardingActivity.class));
        finish();
    }

    @Override
    public void navigateToChildOnboarding(String childId) {
        Intent intent = new Intent(this, ChildOnboardingActivity.class);
        intent.putExtra("CHILD_ID", childId);
        startActivity(intent);
        finish();
    }

}