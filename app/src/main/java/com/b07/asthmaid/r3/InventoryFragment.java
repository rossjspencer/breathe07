package com.b07.asthmaid.r3;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InventoryFragment extends Fragment {

    private enum InventoryType { CONTROLLER, RESCUE }

    // replace with real user id obviously
    private static final String TEMP_USER_ID = "testUserId";
    private static final String CHANNEL_ID = "inventory_alerts";
    private static final String PREFS_NAME = "InventoryAlertPrefs";
    private static final String KEY_SENT_NOTIFICATIONS = "sent_notifications";

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

        createNotificationChannel();

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

    private void loadInventoryFromFirebase() {
        inventoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                controllerItems.clear();
                rescueItems.clear();

                DataSnapshot controllerSnap = snapshot.child("controller").child(TEMP_USER_ID);
                for (DataSnapshot child : controllerSnap.getChildren()) {
                    InventoryItem item = child.getValue(InventoryItem.class);
                    if (item != null) {
                        item.id = child.getKey();
                        item.type = "controller";
                        controllerItems.add(item);
                    }
                }

                DataSnapshot rescueSnap = snapshot.child("rescue").child(TEMP_USER_ID);
                for (DataSnapshot child : rescueSnap.getChildren()) {
                    InventoryItem item = child.getValue(InventoryItem.class);
                    if (item != null) {
                        item.id = child.getKey();
                        item.type = "rescue";
                        rescueItems.add(item);
                    }
                }

                checkAndSendNotifications();
                showCurrentList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("Something went wrong again! Why does this keep happening!?");
            }
        });
    }

    private void checkAndSendNotifications() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> sentNotifications = prefs.getStringSet(KEY_SENT_NOTIFICATIONS, new HashSet<>());
        Set<String> currentAlerts = new HashSet<>();

        int notificationId = 1;

        List<InventoryItem> allItems = new ArrayList<>(controllerItems);
        allItems.addAll(rescueItems);

        for (InventoryItem item : allItems) {
            if (item.id == null) continue;

            // check empty
            if (item.percentLeft <= 0) {
                String alertKey = item.id + "_empty";
                currentAlerts.add(alertKey);
                if (!sentNotifications.contains(alertKey)) {
                    sendNotification(notificationId++, "Empty Medication Warning", 
                            (item.type.equals("controller") ? "Controller" : "Rescue") + " medication " + item.name + " is empty!");
                    sentNotifications.add(alertKey);
                }
            } 
            // check low
            else if (item.isLow()) {
                String alertKey = item.id + "_low";
                currentAlerts.add(alertKey);
                if (!sentNotifications.contains(alertKey)) {
                    sendNotification(notificationId++, "Low Medication Warning", 
                            (item.type.equals("controller") ? "Controller" : "Rescue") + " medication " + item.name + " is running low (" + item.percentLeft + "% left).");
                    sentNotifications.add(alertKey);
                }
            }

            // check expired
            if (item.isExpired()) {
                String alertKey = item.id + "_expired";
                currentAlerts.add(alertKey);
                if (!sentNotifications.contains(alertKey)) {
                    sendNotification(notificationId++, "Expired Medication Warning", 
                            (item.type.equals("controller") ? "Controller" : "Rescue") + " medication " + item.name + " has expired!");
                    sentNotifications.add(alertKey);
                }
            }
        }

        prefs.edit().putStringSet(KEY_SENT_NOTIFICATIONS, currentAlerts).apply();
    }

    private void sendNotification(int id, String title, String content) {
        if (getContext() == null) return;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        notificationManager.notify(id, builder.build());
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
        DatabaseReference typeRef = inventoryRef.child(typeNode).child(TEMP_USER_ID);
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
        checkAndSendNotifications();
        showCurrentList();
    }

    private void showDeleteConfirmDialog(InventoryItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (item.id != null) {
                        // delete from firebase
                        inventoryRef.child(item.type).child(TEMP_USER_ID).child(item.id).removeValue();
                        
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