package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.b07.asthmaid.HomeFragment;
import com.example.smartair.r3.InventoryAlertService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Handle Auth logic
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // If logged in, proceed to dashboard logic
            AuthHelper.handlePostAuth(this, currentUser.getUid());
            
            if (savedInstanceState == null) {
                loadFragment(new HomeFragment());
            }
            return; 
        }

        // 3. Setup Login/Register UI if NOT logged in
        Button loginBtn = findViewById(R.id.login_button_main);
        Button registerBtn = findViewById(R.id.register_button_main);

        if (loginBtn != null) {
            loginBtn.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, LoginActivity.class)));
        }

        if (registerBtn != null) {
            registerBtn.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        // Don't add to back stack for the initial home fragment to avoid blank screen on back press
        transaction.commit();
    }
}