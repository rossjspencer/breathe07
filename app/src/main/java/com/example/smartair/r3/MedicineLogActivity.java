package com.example.smartair.r3;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartair.R;

public class MedicineLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_log);
        
        String childId = getIntent().getStringExtra("CHILD_ID");
        String type = getIntent().getStringExtra(MedicineLogFragment.ARG_TYPE);
        String role = getIntent().getStringExtra(MedicineLogFragment.ARG_ROLE);

        if (savedInstanceState == null) {
            MedicineLogFragment fragment = new MedicineLogFragment();
            Bundle args = new Bundle();
            if (childId != null) {
                args.putString("CHILD_ID", childId);
            }
            if (type != null) {
                args.putString(MedicineLogFragment.ARG_TYPE, type);
            }
            if (role != null) {
                args.putString(MedicineLogFragment.ARG_ROLE, role);
            }
            fragment.setArguments(args);
            
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }
}