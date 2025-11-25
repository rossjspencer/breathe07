package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

public class ProviderOnboardingActivity extends AppCompatActivity {

    Button continueBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_onboarding);

        // 🔒 Security: block access when logged out
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        continueBtn = findViewById(R.id.continue_button);

        continueBtn.setOnClickListener(v -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref = AuthHelper.getUserRef(uid);

            ref.child("onboardingComplete").setValue(true)
                    .addOnCompleteListener(task -> {
                        AuthHelper.redirectUser(ProviderOnboardingActivity.this, "Provider");
                    });
        });
    }
}



