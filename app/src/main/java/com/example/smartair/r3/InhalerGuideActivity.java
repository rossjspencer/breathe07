package com.example.smartair.r3;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartair.R;

public class InhalerGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaler_guide_host);
        
        String childId = getIntent().getStringExtra("CHILD_ID");

        if (savedInstanceState == null) {
            InhalerGuideFragment fragment = new InhalerGuideFragment();
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