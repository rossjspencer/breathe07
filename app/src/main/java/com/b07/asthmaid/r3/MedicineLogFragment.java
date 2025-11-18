package com.b07.asthmaid.r3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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

        displayHandler = new LogDisplayHandler();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(displayHandler);

        showControllerLogs();

        controllerButton.setOnClickListener(v -> showControllerLogs());
        rescueButton.setOnClickListener(v -> showRescueLogs());

        return view;
    }

    private void showControllerLogs() {
        ArrayList<ControllerLogEntry> controllers = medicineLog.getControllerLogs();
        displayHandler.setEntries(controllers);
    }

    private void showRescueLogs() {
        ArrayList<RescueLogEntry> rescues = medicineLog.getRescueLogs();
        displayHandler.setEntries(rescues);
    }

    private void seedDummyData() {
        medicineLog.addEntry(new ControllerLogEntry(2, "2025-11-17 10:00"));
        medicineLog.addEntry(new ControllerLogEntry(1, "2025-11-17 12:15"));
        medicineLog.addEntry(new RescueLogEntry(1, "2025-11-17 09:30"));
    }
}