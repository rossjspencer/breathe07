package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always show the login/register screen; no auto-redirect
        setContentView(R.layout.activity_main);

FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // OPTION A: stay logged in & skip main if already authenticated
        if (currentUser != null) {
            // Use the same logic as after login
            AuthHelper.handlePostAuth(this, currentUser.getUid());
            return; // important: don't show main layout
        }
        
        Button loginBtn = findViewById(R.id.login_button_main);
        Button registerBtn = findViewById(R.id.register_button_main);

        loginBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LoginActivity.class)));

        registerBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
    }
}
