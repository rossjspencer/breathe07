package com.example.smartair;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Spinner roleSpinner;
    private Button registerBtn;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase auth instance
        auth = FirebaseAuth.getInstance();

        // UI references
        emailField = findViewById(R.id.email_edittext);
        passwordField = findViewById(R.id.password_edittext);
        roleSpinner = findViewById(R.id.role_spinner);
        registerBtn = findViewById(R.id.register_button);

        // Load roles from strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.roles_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        registerBtn.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String role = roleSpinner.getSelectedItem().toString();

        // Validate inputs
        if (!AuthHelper.validateCredentials(email, password)) {
            AuthHelper.showToast(this, "Please enter all fields");
            return;
        }

        // Create user in Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        AuthHelper.showToast(this, "Registration failed");
                        return;
                    }

                    // Get user UID
                    String uid = task.getResult().getUser().getUid();

                    // Save user object in Firebase Database
                    User user = new User(email, role);
                    DatabaseReference ref = AuthHelper.getUserRef(uid);

                    ref.setValue(user).addOnCompleteListener(dbTask -> {
                        if (dbTask.isSuccessful()) {
                            AuthHelper.showToast(this, "Registration successful!");
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        } else {
                            AuthHelper.showToast(this, "Database error!");
                        }
                    });
                });
    }
}
