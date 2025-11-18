package com.example.smartair;

import android.widget.Spinner;
import android.widget.ArrayAdapter;
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

public class RegisterActivity extends AppCompatActivity {

    private EditText emailTextView, passwordTextView;
    private Button button;
    private FirebaseAuth auth;

    private Spinner roleSpinner;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        roleSpinner = findViewById(R.id.role_spinner);
        databaseRef = FirebaseDatabase.getInstance().getReference("users");


        // Load roles from strings.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.roles_array,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance();

        emailTextView = findViewById(R.id.email_edittext);
        passwordTextView = findViewById(R.id.password_edittext);
        button = findViewById(R.id.register_button);

        button.setOnClickListener(v -> registerNewUser());
    }

    private void registerNewUser() {
        // Get values from input fields
        String email = emailTextView.getText().toString().trim();
        String password = passwordTextView.getText().toString().trim();
        String selectedRole = roleSpinner.getSelectedItem().toString();


        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter credentials", Toast.LENGTH_LONG).show();
            return;
        }

        // Register new user with Firebase
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String uid = task.getResult().getUser().getUid();
                            String selectedRole = roleSpinner.getSelectedItem().toString();
                            String email = emailTextView.getText().toString().trim();

                            // Create a user object
                            User newUser = new User(email, selectedRole);

                            // Save to database
                            databaseRef.child(uid).setValue(newUser)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_LONG).show();
                                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "Database error!", Toast.LENGTH_LONG).show();
                                        }
                                    });

                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration failed! Please try again later", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}