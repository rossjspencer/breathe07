package com.example.smartair.r3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartair.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsFragment extends Fragment {

    private String currentUserId = "DEFAULT_VALUE";
    private DatabaseReference settingsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        if (getArguments() != null) {
            String argId = getArguments().getString("CHILD_ID");
            if (argId != null && !argId.isEmpty()) {
                currentUserId = argId;
            }
        }

        EditText editBadge1 = view.findViewById(R.id.editBadge1Threshold);
        EditText editBadge3 = view.findViewById(R.id.editBadge3Threshold);
        Button saveButton = view.findViewById(R.id.btnSaveSettings);

        settingsRef = FirebaseDatabase.getInstance().getReference("badge_settings").child(currentUserId);

        // load current values
        settingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer b1 = snapshot.child("badge1_threshold").getValue(Integer.class);
                Integer b3 = snapshot.child("badge3_threshold").getValue(Integer.class);
                
                editBadge1.setText(String.valueOf(b1 != null ? b1 : 10));
                editBadge3.setText(String.valueOf(b3 != null ? b3 : 4));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });


        saveButton.setOnClickListener(v -> {
            String b1Text = editBadge1.getText().toString().trim();
            String b3Text = editBadge3.getText().toString().trim();
            
            if (b1Text.isEmpty() || b3Text.isEmpty()) {
                Toast.makeText(getContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                int b1 = Integer.parseInt(b1Text);
                int b3 = Integer.parseInt(b3Text);
                
                settingsRef.child("badge1_threshold").setValue(b1);
                settingsRef.child("badge3_threshold").setValue(b3);
                        
                Toast.makeText(getContext(), "Settings Saved", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}