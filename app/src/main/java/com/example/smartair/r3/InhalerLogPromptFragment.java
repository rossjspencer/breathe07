package com.example.smartair.r3;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.smartair.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InhalerLogPromptFragment extends Fragment {

    private boolean success;
    private DatabaseReference logReference;
    private DatabaseReference inventoryRef;

    // Default fallback ID
    private String currentUserId = "kqRPXqmnx5NzlrN5CT5L8vrxIhk1"; 
    
    private static final String CHANNEL_ID = "inventory_alerts";

    private enum LogType { CONTROLLER, RESCUE }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inhaler_log_prompt, container, false);

        if (getArguments() != null) {
            success = getArguments().getBoolean("success", false);
            String argId = getArguments().getString("CHILD_ID");
            if (argId != null && !argId.isEmpty()) {
                currentUserId = argId;
            }
        }

        createNotificationChannel();

        logReference = FirebaseDatabase
                .getInstance("https://smartair-a6669-default-rtdb.firebaseio.com")
                .getReference("medicine_logs");

        inventoryRef = FirebaseDatabase.getInstance().getReference("inventory");

        Button skipButton = view.findViewById(R.id.logSkipButton);
        Button okButton = view.findViewById(R.id.logOkButton);

        skipButton.setOnClickListener(v -> navigateToPostCheck());
        okButton.setOnClickListener(v -> showAddLogDialog());

        return view;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Inventory Alerts";
            String description = "Notifications for low or expired medication";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void navigateToPostCheck() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out);
        
        InhalerPostCheckFragment fragment = new InhalerPostCheckFragment();
        Bundle args = new Bundle();
        args.putBoolean("success", success);
        if (getArguments() != null && getArguments().containsKey("CHILD_ID")) {
            args.putString("CHILD_ID", getArguments().getString("CHILD_ID"));
        }
        fragment.setArguments(args);
        
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    // very similar to the medicine log dialog
    private void showAddLogDialog() {
        if (getContext() == null) return;

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_entry, null);

        Spinner spinnerInhalerName = dialogView.findViewById(R.id.spinnerInhalerName);
        EditText editDose = dialogView.findViewById(R.id.editDose);
        RadioGroup typeGroup = dialogView.findViewById(R.id.radioTypeGroup);
        RadioButton radioController = dialogView.findViewById(R.id.radioController);
        RadioButton radioRescue = dialogView.findViewById(R.id.radioRescue);
        Button btnCantFindInhaler = dialogView.findViewById(R.id.btnCantFindInhaler);

        updateSpinner(spinnerInhalerName, LogType.CONTROLLER);

        radioController.setChecked(true);

        // update spinner when type changes
        typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            LogType selectedType = (checkedId == R.id.radioController) ? LogType.CONTROLLER : LogType.RESCUE;
            updateSpinner(spinnerInhalerName, selectedType);
        });

        btnCantFindInhaler.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setMessage("Ask your parent to add your inhaler to their Inventory with a name you'll both remember. Then, it should show up on the list!")
                    .setPositiveButton("OK", null)
                    .show();
        });

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add Log")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // disable the positive button initially
        Button positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
        positiveButton.setEnabled(false);
        positiveButton.setTextColor(Color.GRAY);

        // listener to validate inputs
        Runnable validateInputs = () -> {
            String doseText = editDose.getText().toString().trim();
            boolean hasDose = !doseText.isEmpty();
            
            boolean hasInhaler = false;
            if (spinnerInhalerName.getSelectedItem() != null) {
                String selection = spinnerInhalerName.getSelectedItem().toString();
                if (!selection.equals("No inhalers of this type in Inventory.")) {
                    hasInhaler = true;
                }
            }

            if (hasDose && hasInhaler) {
                positiveButton.setEnabled(true);
                positiveButton.setTextColor(Color.BLUE);
            } else {
                positiveButton.setEnabled(false);
                positiveButton.setTextColor(Color.GRAY);
            }
        };

        editDose.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateInputs.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerInhalerName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                validateInputs.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // set the click listener for the positive button
        positiveButton.setOnClickListener(v -> {
            String doseText = editDose.getText().toString().trim();
            String selectedInhaler = spinnerInhalerName.getSelectedItem().toString();

            int dose;
            try {
                dose = Integer.parseInt(doseText);
            } catch (NumberFormatException e) {
                return;
            }

            // get current time and use for timestamp
            String timestamp = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    java.util.Locale.getDefault()
            ).format(new java.util.Date());

            LogType type;
            int checkedId = typeGroup.getCheckedRadioButtonId();
            if (checkedId == R.id.radioRescue) {
                type = LogType.RESCUE;
            } else {
                type = LogType.CONTROLLER;
            }

            if (type == LogType.CONTROLLER) {
                ControllerLogEntry entry = new ControllerLogEntry(selectedInhaler, dose, timestamp);

                DatabaseReference typeRef = logReference.child("controller")
                        .child(currentUserId);
                String key = typeRef.push().getKey();
                entry.id = key;
                if (key != null) {
                    typeRef.child(key).setValue(entry);
                }
                
                updateInventoryDose(selectedInhaler, dose, "controller");

            } else {
                RescueLogEntry entry = new RescueLogEntry(selectedInhaler, dose, timestamp);

                DatabaseReference typeRef = logReference.child("rescue")
                        .child(currentUserId);
                        String key = typeRef.push().getKey();
                        entry.id = key;
                        if (key != null) {
                            typeRef.child(key).setValue(entry);
                        }
                        
                        updateInventoryDose(selectedInhaler, dose, "rescue");
                    }
                    
                    dialog.dismiss();
                    // after saving, proceed to the post check screen
                    navigateToPostCheck();
        });
    }

    private void updateSpinner(Spinner spinner, LogType type) {
        String typeStr = (type == LogType.CONTROLLER) ? "controller" : "rescue";
        inventoryRef.child(typeStr).child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> names = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    InventoryItem item = child.getValue(InventoryItem.class);
                    if (item != null && item.name != null) {
                        names.add(item.name);
                    }
                }

                if (names.isEmpty()) {
                    names.add("No inhalers of this type in Inventory.");
                }

                if (getContext() != null) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // handle error
            }
        });
    }

    private void updateInventoryDose(String inhalerName, int doseTaken, String type) {
        inventoryRef.child(type).child(currentUserId).orderByChild("name").equalTo(inhalerName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    InventoryItem item = child.getValue(InventoryItem.class);
                    if (item != null) {
                        item.remainingDoses -= doseTaken;
                        if (item.remainingDoses < 0) item.remainingDoses = 0;
                        item.updatePercentLeft();

                        // update firebase
                        child.getRef().setValue(item);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // handle error
            }
        });
    }
}