package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChildOnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_onboarding);

        String childId = getIntent().getStringExtra("CHILD_ID");

        Button continueBtn = findViewById(R.id.child_continue_button);

        continueBtn.setOnClickListener(v -> {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(childId);

            ref.child("onboardingComplete").setValue(true).addOnCompleteListener(task -> {
                Intent i = new Intent(this, ChildHomeActivity.class);
                i.putExtra("CHILD_ID", childId);
                startActivity(i);
                finish();
            });
        });
    }
}

