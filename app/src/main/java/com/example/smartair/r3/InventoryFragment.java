package com.example.smartair.r3;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartair.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InventoryFragment extends Fragment {

    private enum InventoryType { CONTROLLER, RESCUE }

    private static final String TEMP_USER_ID = "testUserId";
    private String currentUserId = TEMP_USER_ID;

    private RecyclerView recyclerView;
    private Button controllerButton;
    private Button rescueButton;
    private Button addItemButton;
    private Button backButton;

    private InventoryDisplayHandler displayHandler;
    private final List<InventoryItem> controllerItems = new ArrayList<>();
    private final List<InventoryItem> rescueItems = new ArrayList<>();
    private InventoryType currentType = InventoryType.CONTROLLER;

    private DatabaseReference inventoryRef;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(getContext(), "Notifications permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        if (getArguments() != null) {
            String argId = getArguments().getString("CHILD_ID");
            if (argId != null && !argId.isEmpty()) {
                currentUserId = argId;
            }
        }

        //might not be necessary?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        inventoryRef = FirebaseDatabase.getInstance()
                .getReference("inventory");

        recyclerView = view.findViewById(R.id.inventoryRecyclerView);
        controllerButton = view.findViewById(R.id.inventoryControllerButton);
        rescueButton = view.findViewById(R.id.inventoryRescueButton);
        addItemButton = view.findViewById(R.id.inventoryAddButton);
        backButton = view.findViewById(R.id.backButton);

        displayHandler = new InventoryDisplayHandler();
        displayHandler.setOnDeleteClickListener(this::showDeleteConfirmDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(displayHandler);

        controllerButton.setOnClickListener(v -> {
            currentType = InventoryType.CONTROLLER;
            showCurrentList();
        });

        rescueButton.setOnClickListener(v -> {
            currentType = InventoryType.RESCUE;
            showCurrentList();
        });

        addItemButton.setOnClickListener(v -> showAddItemDialog());

        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        loadInventoryFromFirebase();

        return view;
    }

    private void loadInventoryFromFirebase() {
        inventoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                controllerItems.clear();
                rescueItems.clear();

                DataSnapshot controllerSnap = snapshot.child("controller").child(currentUserId);
                for (DataSnapshot child : controllerSnap.getChildren()) {
                    InventoryItem item = child.getValue(InventoryItem.class);
                    if (item != null) {
                        item.id = child.getKey();
                        item.type = "controller";
                        controllerItems.add(item);
                    }
                }

                DataSnapshot rescueSnap = snapshot.child("rescue").child(currentUserId);
                for (DataSnapshot child : rescueSnap.getChildren()) {
                    InventoryItem item = child.getValue(InventoryItem.class);
                    if (item != null) {
                        item.id = child.getKey();
                        item.type = "rescue";
                        rescueItems.add(item);
                    }
                }

                showCurrentList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("Something went wrong again! Why does this keep happening!?");
            }
        });
    }

    private void showCurrentList() {
        if (currentType == InventoryType.CONTROLLER) {
            displayHandler.setItems(controllerItems);
        } else {
            displayHandler.setItems(rescueItems);
        }
    }

    private void showAddItemDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.fragment_add_inventory_dialog, null);

        EditText nameEdit = dialogView.findViewById(R.id.editInventoryName);
        EditText purchaseEdit = dialogView.findViewById(R.id.editPurchaseDate);
        EditText expiryEdit = dialogView.findViewById(R.id.editExpiryDate);
        EditText capacityEdit = dialogView.findViewById(R.id.editDoseCapacity);
        EditText remainingEdit = dialogView.findViewById(R.id.editRemainingDoses);
        RadioGroup typeGroup = dialogView.findViewById(R.id.radioInventoryTypeGroup);
        RadioButton controllerRadio = dialogView.findViewById(R.id.radioInventoryController);
        RadioButton rescueRadio = dialogView.findViewById(R.id.radioInventoryRescue);

        if (currentType == InventoryType.CONTROLLER) {
            controllerRadio.setChecked(true);
        } else {
            rescueRadio.setChecked(true);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Inventory Item")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameEdit.getText().toString().trim();
                    String purchase = purchaseEdit.getText().toString().trim();
                    String expiry = expiryEdit.getText().toString().trim();
                    String capacityStr = capacityEdit.getText().toString().trim();
                    String remainingStr = remainingEdit.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(capacityStr)) {
                        Toast.makeText(getContext(), "Name and Capacity are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int capacity;
                    try {
                        capacity = Integer.parseInt(capacityStr);
                    } catch (NumberFormatException e) {
                        return;
                    }

                    int remaining;
                    if (TextUtils.isEmpty(remainingStr)) {
                        remaining = capacity;
                    } else {
                        try {
                            remaining = Integer.parseInt(remainingStr);
                        } catch (NumberFormatException e) {
                            return;
                        }
                    }

                    String type = (typeGroup.getCheckedRadioButtonId()
                            == R.id.radioInventoryRescue) ? "rescue" : "controller";

                    InventoryItem item = new InventoryItem(
                            name,
                            type,
                            purchase,
                            expiry,
                            capacity,
                            remaining
                    );

                    saveItemToFirebase(item);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveItemToFirebase(InventoryItem item) {
        String typeNode = item.type; // rescue or controller
        DatabaseReference typeRef = inventoryRef.child(typeNode).child(currentUserId);
        String key = typeRef.push().getKey();
        item.id = key;
        if (key != null) {
            typeRef.child(key).setValue(item);
        }

        // update local
        if ("controller".equals(typeNode)) {
            controllerItems.add(item);
        } else {
            rescueItems.add(item);
        }
        showCurrentList();
    }

    private void showDeleteConfirmDialog(InventoryItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (item.id != null) {
                        // delete from firebase
                        inventoryRef.child(item.type).child(currentUserId).child(item.id).removeValue();
                        
                        // delete from local list
                        if ("controller".equals(item.type)) {
                            controllerItems.remove(item);
                        } else {
                            rescueItems.remove(item);
                        }
                        
                        // update view
                        showCurrentList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}