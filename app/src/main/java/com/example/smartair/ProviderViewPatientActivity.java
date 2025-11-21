package com.example.smartair;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
// WHOLE RHING IS A PALACEHOIIEDERE!!!
public class ProviderViewPatientActivity extends AppCompatActivity {

    private String childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_view_patient);

        // Get the ID passed from the adapter
        childId = getIntent().getStringExtra("CHILD_ID");

        TextView title = findViewById(R.id.tvPatientTitle);
        TextView placeholder = findViewById(R.id.tv_logs_placeholder);

        if (childId != null) {
            title.setText("Patient Record");
            // Will connect this to the sharingSettings and logs nodes later
            placeholder.setText("Viewing Shared Data for Child ID:\n" + childId + "\n\n(Charts and Logs will be linked here)");
        } else {
            placeholder.setText("Error: No Patient ID found.");
        }
    }
}