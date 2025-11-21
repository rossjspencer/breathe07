package com.example.smartair;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ParentViewChildActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_view_child);

        // Retrieve the Child ID passed from the list
        String childId = getIntent().getStringExtra("CHILD_ID");

        // show the ID (temp)
        if(childId != null) {
            TextView title = findViewById(R.id.tvChildTitle);
            title.setText("Viewing Child ID: " + childId);
        }
    }
}