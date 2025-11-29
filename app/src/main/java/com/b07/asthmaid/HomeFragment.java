package com.b07.asthmaid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.b07.asthmaid.r3.BadgesFragment;
import com.b07.asthmaid.r3.InhalerGuideFragment;
import com.b07.asthmaid.r3.InventoryFragment;
import com.b07.asthmaid.r3.MedicineLogFragment;
import com.b07.asthmaid.r3.SettingsFragment;

public class HomeFragment extends Fragment {

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    System.out.println("guh");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home_fragment, container, false);

        checkAndRequestPermissions();

        Button buttonMedicineLog = view.findViewById(R.id.buttonMedicineLog);
        Button buttonInventory = view.findViewById(R.id.buttonInventory);
        Button buttonGuide = view.findViewById(R.id.buttonGuide);
        Button buttonBadges = view.findViewById(R.id.buttonBadges);
        Button buttonSettings = view.findViewById(R.id.buttonSettings);

        buttonInventory.setOnClickListener(v -> loadFragment(new InventoryFragment()));
        buttonMedicineLog.setOnClickListener(v -> loadFragment(new MedicineLogFragment()));
        buttonGuide.setOnClickListener(v -> loadFragment(new InhalerGuideFragment()));
        buttonBadges.setOnClickListener(v -> loadFragment(new BadgesFragment()));
        
        if (buttonSettings != null) {
            buttonSettings.setOnClickListener(v -> loadFragment(new SettingsFragment()));
        }

        return view;
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (getContext() != null && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        if (getParentFragmentManager() != null) {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}