package com.b07.asthmaid.r3;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.b07.asthmaid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MedicineLogFragment extends Fragment {

    private LogDisplayHandler displayHandler;
    private MedicineLog medicineLog;
    private DatabaseReference logReference;
    //String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

    // remember what log type is being shown
    private enum LogType { CONTROLLER, RESCUE }
    private LogType currentType = LogType.CONTROLLER;

    public final String TEMP_ID = "kqRPXqmnx5NzlrN5CT5L8vrxIhk1";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_log_view, container, false);

        logReference = FirebaseDatabase
                .getInstance("https://smartair-a6669-default-rtdb.firebaseio.com")
                .getReference("medicine_logs");
        medicineLog = new MedicineLog();

        //seedDummyData(); testing purposes only
        loadLogsFromFirebase();

        RecyclerView recyclerView = view.findViewById(R.id.logRecyclerView);
        Button controllerButton = view.findViewById(R.id.controllerButton);
        Button rescueButton = view.findViewById(R.id.rescueButton);
        Button addEntryButton = view.findViewById(R.id.addEntryButton);

        displayHandler = new LogDisplayHandler(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(displayHandler);

        showControllerLogs();

        controllerButton.setOnClickListener(v -> showControllerLogs());
        rescueButton.setOnClickListener(v -> showRescueLogs());
        addEntryButton.setOnClickListener(v -> showAddLogDialog());

        return view;
    }

    private void showControllerLogs() {
        ArrayList<ControllerLogEntry> controllers = medicineLog.getControllerLogs();
        displayHandler.setEntries(controllers);
        currentType = LogType.CONTROLLER;
    }

    private void showRescueLogs() {
        ArrayList<RescueLogEntry> rescues = medicineLog.getRescueLogs();
        displayHandler.setEntries(rescues);
        currentType = LogType.RESCUE;
    }

    private void seedDummyData() {
        medicineLog.addEntry(new ControllerLogEntry(2, "2025-11-17 10:00"));
        medicineLog.addEntry(new ControllerLogEntry(1, "2025-11-17 12:15"));
        medicineLog.addEntry(new RescueLogEntry(1, "2025-11-17 09:30"));
    }

    private void showAddLogDialog() {

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_entry, null);

        EditText editDose = dialogView.findViewById(R.id.editDose);
        RadioGroup typeGroup = dialogView.findViewById(R.id.radioTypeGroup);
        RadioButton radioController = dialogView.findViewById(R.id.radioController);
        RadioButton radioRescue = dialogView.findViewById(R.id.radioRescue);

        // the currently viewed log type is pre-selected
        if (currentType == LogType.CONTROLLER) {
            radioController.setChecked(true);
        } else {
            radioRescue.setChecked(true);
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add Log")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String doseText = editDose.getText().toString().trim();
                    if (doseText.isEmpty()) {
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
                        ControllerLogEntry entry = new ControllerLogEntry(dose, timestamp);

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

                    } else {
                        RescueLogEntry entry = new RescueLogEntry(dose, timestamp);

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
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showDeleteConfirmDialog(MedicineLogEntry entry) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this entry?")
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