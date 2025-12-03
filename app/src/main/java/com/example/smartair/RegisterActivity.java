package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirst, etLast, etProviderName, etEmail, etPass;
    private LinearLayout layoutNames;
    private Spinner roleSpinner;
    private Button btnRegister;
    private FirebaseAuth auth;
    private DatabaseReference db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference("users");

        bindViews();
        setupSpinner();

        btnRegister.setOnClickListener(v -> checkInputs());
    }

    private void bindViews() {
        etFirst = findViewById(R.id.firstname_edittext);
        etLast = findViewById(R.id.lastname_edittext);
        etProviderName = findViewById(R.id.providername_edittext);
        etEmail = findViewById(R.id.email_edittext);
        etPass = findViewById(R.id.password_edittext);
        layoutNames = findViewById(R.id.layout_names);
        roleSpinner = findViewById(R.id.role_spinner);
        btnRegister = findViewById(R.id.register_button);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.roles_array, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String role = parent.getItemAtPosition(position).toString();
                if (role.equals("Provider")) {
                    layoutNames.setVisibility(View.GONE);
                    etProviderName.setVisibility(View.VISIBLE);
                } else {
                    layoutNames.setVisibility(View.VISIBLE);
                    etProviderName.setVisibility(View.GONE);
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void checkInputs() {
        String role = roleSpinner.getSelectedItem().toString();
        String email = etEmail.getText().toString().trim();
        String pass = etPass.getText().toString().trim();


        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Email/Password required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        String passwordPattern = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$";
        if (!pass.matches(passwordPattern)) {
            Toast.makeText(
                    this,
                    "Password must be 8+ characters, include upper, lower and number",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // Provider registration flow
        if (role.equals("Provider")) {
            String pName = etProviderName.getText().toString().trim();

            if (pName.isEmpty()) {
                Toast.makeText(this, "Provider Name required", Toast.LENGTH_SHORT).show();
                return;
            }

            // CHECK UNIQUENESS
            db.orderByChild("providerName").equalTo(pName)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(RegisterActivity.this,
                                        "Provider Name already taken", Toast.LENGTH_SHORT).show();
                            } else {
                                registerUser(role, email, pass, null, null, pName);
                            }
                        }

                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });

        } else { // Parent registration flow
            String first = etFirst.getText().toString().trim();
            String last = etLast.getText().toString().trim();

            if (first.isEmpty() || last.isEmpty()) {
                Toast.makeText(this, "Names required", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(role, email, pass, first, last, null);
        }
    }

    private void registerUser(String role, String email, String pass,
                              String first, String last, String pName) {

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        Toast.makeText(this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String uid = task.getResult().getUser().getUid();
                    User user = new User(uid, role, email);

                    if (role.equals("Provider")) {
                        user.providerName = pName;
                    } else {
                        user.firstName = first;
                        user.lastName = last;
                    }

                    // Set onboarding flag for ALL new accounts
                    user.onboardingComplete = false;

                    db.child(uid).setValue(user).addOnSuccessListener(v -> {

                        // Alternatively, if User class does NOT have onboardingComplete:
                        // db.child(uid).child("onboardingComplete").setValue(false);

                        DatabaseReference statsRef = FirebaseDatabase.getInstance()
                                .getReference("guide_stats")
                                .child(uid);

                        statsRef.child("accountCreationDate")
                                .setValue(ServerValue.TIMESTAMP);

                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    });
                });
    }

}


