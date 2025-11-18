package com.b07.asthmaid.r3;

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

import java.util.ArrayList;

public class MedicineLogFragment extends Fragment {

    private LogDisplayHandler displayHandler;
    private MedicineLog medicineLog;

    // remember what log type is being shown
    private enum LogType { CONTROLLER, RESCUE }
    private LogType currentType = LogType.CONTROLLER;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_log_view, container, false);

        medicineLog = new MedicineLog();
        seedDummyData();

        RecyclerView recyclerView = view.findViewById(R.id.logRecyclerView);
        Button controllerButton = view.findViewById(R.id.controllerButton);
        Button rescueButton = view.findViewById(R.id.rescueButton);
        Button addEntryButton = view.findViewById(R.id.addEntryButton);

        displayHandler = new LogDisplayHandler();
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
                        // super simple: you could show a Toast instead
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
                        medicineLog.addEntry(entry);
                        if (currentType == LogType.CONTROLLER) {
                            showControllerLogs();
                        }
                    } else {
                        RescueLogEntry entry = new RescueLogEntry(dose, timestamp);
                        medicineLog.addEntry(entry);
                        if (currentType == LogType.RESCUE) {
                            showRescueLogs();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}