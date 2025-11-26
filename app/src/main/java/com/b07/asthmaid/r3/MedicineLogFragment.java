package com.b07.asthmaid.r3;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b07.asthmaid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MedicineLogFragment extends Fragment {

    private LogDisplayHandler displayHandler;
    private MedicineLog medicineLog;
    private DatabaseReference logReference;
    private DatabaseReference inventoryRef;
    private Button controllerButton;
    private Button rescueButton;

    // remember what log type is being shown
    private enum LogType { CONTROLLER, RESCUE }
    private LogType currentType = LogType.CONTROLLER;

    public final String TEMP_ID = "kqRPXqmnx5NzlrN5CT5L8vrxIhk1";
    private static final String TEMP_USER_ID = "testUserId"; // change this later ay ay ay whoa

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_log_view, container, false);

        logReference = FirebaseDatabase
                .getInstance("https://smartair-a6669-default-rtdb.firebaseio.com")
                .getReference("medicine_logs");
        
        inventoryRef = FirebaseDatabase.getInstance().getReference("inventory");
        
        medicineLog = new MedicineLog();

        loadLogsFromFirebase();

        RecyclerView recyclerView = view.findViewById(R.id.logRecyclerView);
        Button backButton = view.findViewById(R.id.backButton);
        controllerButton = view.findViewById(R.id.controllerButton);
        rescueButton = view.findViewById(R.id.rescueButton);
        Button addEntryButton = view.findViewById(R.id.addEntryButton);

        displayHandler = new LogDisplayHandler(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(displayHandler);

        showControllerLogs();

        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });
        controllerButton.setOnClickListener(v -> showControllerLogs());
        rescueButton.setOnClickListener(v -> showRescueLogs());
        addEntryButton.setOnClickListener(v -> showAddLogDialog());

        return view;
    }

    private void updateButtonStyles() {
        if (currentType == LogType.CONTROLLER) {
            controllerButton.setBackgroundColor(Color.GRAY);
            controllerButton.setTextColor(Color.WHITE);
            rescueButton.setBackgroundColor(0xFF6200EE); // can be changed later
            rescueButton.setTextColor(Color.WHITE);
        } else {
            rescueButton.setBackgroundColor(Color.GRAY);
            rescueButton.setTextColor(Color.WHITE);
            controllerButton.setBackgroundColor(0xFF6200EE);
            controllerButton.setTextColor(Color.WHITE);
        }
    }

    private void showControllerLogs() {
        ArrayList<ControllerLogEntry> controllers = medicineLog.getControllerLogs();
        displayHandler.setEntries(controllers);
        currentType = LogType.CONTROLLER;
        updateButtonStyles();
    }

    private void showRescueLogs() {
        ArrayList<RescueLogEntry> rescues = medicineLog.getRescueLogs();
        displayHandler.setEntries(rescues);
        currentType = LogType.RESCUE;
        updateButtonStyles();
    }

    private void showAddLogDialog() {

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_entry, null);

        Spinner spinnerInhalerName = dialogView.findViewById(R.id.spinnerInhalerName);
        EditText editDose = dialogView.findViewById(R.id.editDose);
        RadioGroup typeGroup = dialogView.findViewById(R.id.radioTypeGroup);
        RadioButton radioController = dialogView.findViewById(R.id.radioController);
        RadioButton radioRescue = dialogView.findViewById(R.id.radioRescue);
        Button btnCantFindInhaler = dialogView.findViewById(R.id.btnCantFindInhaler);

        // populate spinner initially based on current view type
        updateSpinner(spinnerInhalerName, currentType);

        // pre-select type
        if (currentType == LogType.CONTROLLER) {
            radioController.setChecked(true);
        } else {
            radioRescue.setChecked(true);
        }

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

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add Log")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String doseText = editDose.getText().toString().trim();
                    
                    String selectedInhaler = null;
                    if (spinnerInhalerName.getSelectedItem() != null) {
                        String selection = spinnerInhalerName.getSelectedItem().toString();
                        // check if it's the empty placeholder
                        if (!selection.equals("No inhalers of this type in Inventory.")) {
                            selectedInhaler = selection;
                        }
                    }

                    if (doseText.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter dose", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (selectedInhaler == null || selectedInhaler.isEmpty()) {
                        Toast.makeText(getContext(), "Please select an inhaler", Toast.LENGTH_SHORT).show();
                        return;
                    }

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

                        //path can be changed
                        DatabaseReference typeRef = logReference.child("controller")
                                .child(TEMP_ID);  // hardcoded for testing
                        String key = typeRef.push().getKey();
                        entry.id = key;
                        if (key != null) {
                            typeRef.child(key).setValue(entry);
                        }

                        // sync local
                        medicineLog.addEntry(entry);
                        if (currentType == LogType.CONTROLLER) {
                            showControllerLogs();
                        }
                        
                        updateInventoryDose(selectedInhaler, dose, "controller");

                    } else {
                        RescueLogEntry entry = new RescueLogEntry(selectedInhaler, dose, timestamp);

                        //path can be changed
                        DatabaseReference typeRef = logReference.child("rescue")
                                .child(TEMP_ID);  // hardcoded for testing;
                        String key = typeRef.push().getKey();
                        entry.id = key;
                        if (key != null) {
                            typeRef.child(key).setValue(entry);
                        }

                        medicineLog.addEntry(entry);
                        if (currentType == LogType.RESCUE) {
                            showRescueLogs();
                        }
                        
                        updateInventoryDose(selectedInhaler, dose, "rescue");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void updateInventoryDose(String inhalerName, int doseTaken, String type) {
        inventoryRef.child(type).child(TEMP_USER_ID).orderByChild("name").equalTo(inhalerName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    InventoryItem item = child.getValue(InventoryItem.class);
                    if (item != null) {
                        // update remaining doses
                        item.remainingDoses -= doseTaken;
                        if (item.remainingDoses < 0) item.remainingDoses = 0;
                        
                        // recalculate percentage
                        item.updatePercentLeft();
                        
                        // update Firebase
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
    
    private void restoreInventoryDose(String inhalerName, int doseToRestore, String type) {
        inventoryRef.child(type).child(TEMP_USER_ID).orderByChild("name").equalTo(inhalerName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    InventoryItem item = child.getValue(InventoryItem.class);
                    if (item != null) {
                        // restore doses
                        item.remainingDoses += doseToRestore;
                        if (item.remainingDoses > item.doseCapacity) item.remainingDoses = item.doseCapacity;
                        
                        // recalculate percentage
                        item.updatePercentLeft();
                        
                        // update firebase
                        child.getRef().setValue(item);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // handle error
                System.out.println("AAAAAAAAHHHHHH!!! HELPPP!!! HELP ME! HELP! HELP! HEEEEEEEELP!");
            }
        });
    }
    
    private void updateSpinner(Spinner spinner, LogType type) {
        String typeStr = (type == LogType.CONTROLLER) ? "controller" : "rescue";
        inventoryRef.child(typeStr).child(TEMP_USER_ID).addListenerForSingleValueEvent(new ValueEventListener() {
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
                System.out.println("bad thing happened.");
            }
        });
    }

    public void showDeleteConfirmDialog(MedicineLogEntry entry) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this entry?\n\nThis will restore the doses you took on your Parent's Inventory page!")
                .setPositiveButton("Delete", (dialog, which) -> {

                    // remove locally
                    medicineLog.removeEntry(entry);

                    // try to remove from firebase
                    if (entry.id != null) {
                        String typeNode = (entry instanceof ControllerLogEntry)
                                ? "controller"
                                : "rescue";

                        logReference.child(typeNode)
                                .child(TEMP_ID)
                                .child(entry.id)
                                .removeValue();
                                
                        // restore doses in inventory
                        if (entry.name != null) {
                            restoreInventoryDose(entry.name, entry.doseCount, typeNode);
                        }
                    }

                    // refresh current view
                    if (currentType == LogType.CONTROLLER) {
                        showControllerLogs();
                    } else {
                        showRescueLogs();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadLogsFromFirebase() {
        logReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // clear local log
                medicineLog.clear();

                DataSnapshot controllerSnap = snapshot.child("controller").child(TEMP_ID);
                for (DataSnapshot child : controllerSnap.getChildren()) {
                    ControllerLogEntry entry = child.getValue(ControllerLogEntry.class);
                    if (entry != null) {
                        entry.id = child.getKey();
                        medicineLog.addEntry(entry);
                    }
                }

                DataSnapshot rescueSnap = snapshot.child("rescue").child(TEMP_ID);
                for (DataSnapshot child : rescueSnap.getChildren()) {
                    RescueLogEntry entry = child.getValue(RescueLogEntry.class);
                    if (entry != null) {
                        entry.id = child.getKey();
                        medicineLog.addEntry(entry);
                    }
                }

                // refresh list
                if (currentType == LogType.CONTROLLER) {
                    showControllerLogs();
                } else {
                    showRescueLogs();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("Something went wrong. Uh oh! Someone oughta fix that!");
            }
        });
    }
}