package com.example.smartair;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ParentHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_home);

        Button logButton = findViewById(R.id.log_symptoms_button);
        logButton.setOnClickListener(v -> {
            startActivity(new Intent(ParentHomeActivity.this, DailyLogActivity.class));
        });
    }
}
