package com.example.smartair.r3;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartair.R;

public class BadgeSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_badge_settings);
        
        String childId = getIntent().getStringExtra("CHILD_ID");

        if (savedInstanceState == null) {
            SettingsFragment fragment = new SettingsFragment();
            if (childId != null) {
                Bundle args = new Bundle();
                args.putString("CHILD_ID", childId);
                fragment.setArguments(args);
            }
            
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }
}