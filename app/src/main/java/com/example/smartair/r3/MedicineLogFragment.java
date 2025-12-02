package com.example.smartair.r3;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MedicineLogFragment extends Fragment {

    public static final String ARG_TYPE = "arg_type";
    public static final String ARG_ROLE = "arg_role";
    private static final String BADGE_CHANNEL_ID = "badge_alerts";
    private static final String CHANNEL_ID = "inventory_alerts";
    private static final String TAG = "MedicineLogFragment";

    private LogDisplayHandler displayHandler;
    private MedicineLog medicineLog;
    private DatabaseReference logReference;
    private DatabaseReference inventoryRef;
    private Button controllerButton;
    private Button rescueButton;

    private enum LogType { CONTROLLER, RESCUE }
    private LogType currentType = LogType.CONTROLLER;
    private boolean isTypeLocked = false;

    // default id, can be set to "" byt default after testing
    private String currentUserId = "kqRPXqmnx5NzlrN5CT5L8vrxIhk1"; 
    private String userRole = "Child"; // Default

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_log_view, container, false);

        if (getArguments() != null) {
            String type = getArguments().getString(ARG_TYPE);
            if (type != null) {
                if (type.equals("RESCUE")) {
                    currentType = LogType.RESCUE;
                    isTypeLocked = true;
                } else if (type.equals("CONTROLLER")) {
                    currentType = LogType.CONTROLLER;
                    isTypeLocked = true;
                }
            }
            
            String childId = getArguments().getString("CHILD_ID");
            if (childId != null && !childId.isEmpty()) {
                currentUserId = childId;
            }
            
            String role = getArguments().getString(ARG_ROLE);
            if (role != null) {
                userRole = role;
            }
        }
        
        // fetch actual role from firebase auth user
        // this will log null for a child login, but this is okay since the medicinelog help button defaults to the child message
        Log.d(TAG, "Checking FirebaseAuth...");
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Log.d(TAG, "User logged in: " + uid);
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            userRef.child("role").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String r = snapshot.getValue(String.class);
                    if (r != null) {
                        userRole = r;
                        Log.d(TAG, "Fetched role: " + r);
                        if (getContext() != null) {
                        }
                    } else {
                        Log.d(TAG, "Role is null in DB");
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Role fetch cancelled: " + error.getMessage());
                }
            });
        } else {
            Log.d(TAG, "FirebaseAuth user is null");
        }

        createInventoryNotificationChannel();

        logReference = FirebaseDatabase
                .getInstance("https://smartair-a6669-default-rtdb.firebaseio.com")
                .getReference("medicine_logs");
        
        inventoryRef = FirebaseDatabase.getInstance().getReference("inventory");
        
        medicineLog = new MedicineLog();

        loadLogsFromFirebase();

        RecyclerView recyclerView = view.findViewById(R.id.logRecyclerView);
        controllerButton = view.findViewById(R.id.controllerButton);
        rescueButton = view.findViewById(R.id.rescueButton);
        Button addEntryButton = view.findViewById(R.id.addEntryButton);

        displayHandler = new LogDisplayHandler(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(displayHandler);

        if (isTypeLocked) {
            controllerButton.setVisibility(View.GONE);
            rescueButton.setVisibility(View.GONE);
        } else {
            controllerButton.setOnClickListener(v -> showControllerLogs());
            rescueButton.setOnClickListener(v -> showRescueLogs());
        }

        // ensure initial view is correct
        if (currentType == LogType.CONTROLLER) {
            showControllerLogs();
        } else {
            showRescueLogs();
        }

        
        addEntryButton.setOnClickListener(v -> showAddLogDialog());

        return view;
    }

    private void updateButtonStyles() {
        if (isTypeLocked) return; 

        if (currentType == LogType.CONTROLLER) {
            controllerButton.setBackgroundColor(Color.GRAY);
            controllerButton.setTextColor(Color.WHITE);
            rescueButton.setBackgroundColor(0xFF6200EE);
            rescueButton.setTextColor(Color.WHITE);
        } else {
            rescueButton.setBackgroundColor(Color.GRAY);
            rescueButton.setTextColor(Color.WHITE);
            controllerButton.setBackgroundColor(0xFF6200EE);
            controllerButton.setTextColor(Color.WHITE);
        }
    }

    private void showControllerLogs() {
        if (!isTypeLocked) currentType = LogType.CONTROLLER;
        ArrayList<ControllerLogEntry> controllers = medicineLog.getControllerLogs();
        displayHandler.setEntries(controllers);
        updateButtonStyles();
    }

    private void showRescueLogs() {
        if (!isTypeLocked) currentType = LogType.RESCUE;
        ArrayList<RescueLogEntry> rescues = medicineLog.getRescueLogs();
        displayHandler.setEntries(rescues);
        updateButtonStyles();
    }

    private void showAddLogDialog() {

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_entry, null);

        Spinner spinnerInhalerName = dialogView.findViewById(R.id.spinnerInhalerName);
        EditText editDose = dialogView.findViewById(R.id.editDose);
        Button btnCantFindInhaler = dialogView.findViewById(R.id.btnCantFindInhaler);
        
        // type selection is hidden except in the post-dose log prompt because the inhaler type is pre-chosen
        TextView typeLabel = dialogView.findViewById(R.id.tvTypeLabel);
        RadioGroup typeGroup = dialogView.findViewById(R.id.radioTypeGroup);
        if (typeLabel != null) typeLabel.setVisibility(View.GONE);
        if (typeGroup != null) typeGroup.setVisibility(View.GONE);

        updateSpinner(spinnerInhalerName, currentType);

        // help text changes depending on user role
        if ("Parent".equals(userRole)) {
            btnCantFindInhaler.setText("Can't find your child's inhaler?");
            btnCantFindInhaler.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setMessage("Check the Inventory page and make sure the inhaler you're looking for is added. If not, make sure to add it so you and your child can log doses!")
                        .setPositiveButton("OK", null)
                        .show();
            });
        } else {
            // child logic (default)
            btnCantFindInhaler.setText("Can't find your inhaler?");
            btnCantFindInhaler.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setMessage("Ask your parent to add your inhaler to their Inventory with a name you'll both remember. Then, it should show up on the list!")
                        .setPositiveButton("OK", null)
                        .show();
            });
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add Log")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String doseText = editDose.getText().toString().trim();
                    
                    String selectedInhaler = null;
                    if (spinnerInhalerName.getSelectedItem() != null) {
                        String selection = spinnerInhalerName.getSelectedItem().toString();
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

                    String timestamp = new java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            java.util.Locale.getDefault()
                    ).format(new java.util.Date());

                    // use currentType directly
                    LogType type = currentType;

                    if (type == LogType.CONTROLLER) {
                        ControllerLogEntry entry = new ControllerLogEntry(selectedInhaler, dose, timestamp);

                        DatabaseReference typeRef = logReference.child("controller").child(currentUserId);
                        String key = typeRef.push().getKey();
                        entry.id = key;
                        if (key != null) {
                            typeRef.child(key).setValue(entry);
                        }

                        updateInventoryDose(selectedInhaler, dose, "controller");
                        
                        // check for badge
                        checkControllerStreak(currentUserId);

                    } else {
                        RescueLogEntry entry = new RescueLogEntry(selectedInhaler, dose, timestamp);

                        DatabaseReference typeRef = logReference.child("rescue").child(currentUserId);
                        String key = typeRef.push().getKey();
                        entry.id = key;
                        if (key != null) {
                            typeRef.child(key).setValue(entry);
                        }

                        updateInventoryDose(selectedInhaler, dose, "rescue");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void updateInventoryDose(String inhalerName, int doseTaken, String type) {
        inventoryRef.child(type).child(currentUserId).orderByChild("name").equalTo(inhalerName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        DataSnapshot remainingSnap = child.child("remainingDoses");
                        DataSnapshot capacitySnap = child.child("doseCapacity");
                        
                        if (remainingSnap.exists() && capacitySnap.exists()) {
                            Integer remaining = remainingSnap.getValue(Integer.class);
                            Integer capacity = capacitySnap.getValue(Integer.class);
                            
                            if (remaining != null && capacity != null) {
                                remaining -= doseTaken;
                                if (remaining < 0) remaining = 0;
                                
                                int percent = (int)((remaining / (float)capacity) * 100);
                                
                                child.getRef().child("remainingDoses").setValue(remaining);
                                child.getRef().child("percentLeft").setValue(percent);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void restoreInventoryDose(String inhalerName, int doseToRestore, String type) {
        inventoryRef.child(type).child(currentUserId).orderByChild("name").equalTo(inhalerName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        DataSnapshot remainingSnap = child.child("remainingDoses");
                        DataSnapshot capacitySnap = child.child("doseCapacity");
                        
                        if (remainingSnap.exists() && capacitySnap.exists()) {
                            Integer remaining = remainingSnap.getValue(Integer.class);
                            Integer capacity = capacitySnap.getValue(Integer.class);
                            
                            if (remaining != null && capacity != null) {
                                remaining += doseToRestore;
                                if (remaining > capacity) remaining = capacity;
                                
                                int percent = (int)((remaining / (float)capacity) * 100);
                                
                                child.getRef().child("remainingDoses").setValue(remaining);
                                child.getRef().child("percentLeft").setValue(percent);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void showDeleteConfirmDialog(MedicineLogEntry entry) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this entry?\n\nThis will restore the doses that were taken on the Inventory page!")
                .setPositiveButton("Delete", (dialog, which) -> {

                    medicineLog.removeEntry(entry);

                    if (entry.id != null) {
                        String typeNode = (entry instanceof ControllerLogEntry)
                                ? "controller"
                                : "rescue";

                        logReference.child(typeNode).child(currentUserId).child(entry.id).removeValue();
                                
                        if (entry.name != null) {
                            restoreInventoryDose(entry.name, entry.doseCount, typeNode);
                        }
                    }

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
                if (medicineLog == null) return; 
                medicineLog.clear();

                DataSnapshot controllerSnap = snapshot.child("controller").child(currentUserId);
                for (DataSnapshot child : controllerSnap.getChildren()) {
                    ControllerLogEntry entry = child.getValue(ControllerLogEntry.class);
                    if (entry != null) {
                        entry.id = child.getKey();
                        medicineLog.addEntry(entry);
                    }
                }

                DataSnapshot rescueSnap = snapshot.child("rescue").child(currentUserId);
                for (DataSnapshot child : rescueSnap.getChildren()) {
                    RescueLogEntry entry = child.getValue(RescueLogEntry.class);
                    if (entry != null) {
                        entry.id = child.getKey();
                        medicineLog.addEntry(entry);
                    }
                }

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
    
    // badge logic
    private void checkControllerStreak(String childId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(childId);
        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("medicine_logs").child("controller").child(childId);
        DatabaseReference statsRef = FirebaseDatabase.getInstance().getReference("guide_stats").child(childId);

        userRef.child("plannedSchedule").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot scheduleSnap) {
                Map<String, Integer> schedule = new HashMap<>();
                for (DataSnapshot d : scheduleSnap.getChildren()) {
                    Integer val = d.getValue(Integer.class);
                    if (d.getKey() != null && val != null) {
                        schedule.put(d.getKey(), val);
                    }
                }
                
                logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot logsSnap) {
                        if (calculateStreak(schedule, logsSnap)) {
                            statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot statsSnap) {
                                    GuideStats stats = statsSnap.getValue(GuideStats.class);
                                    if (stats == null) stats = new GuideStats(0, 0, "", "");
                                    
                                    if (!stats.hasBadge("badge_perfect_week")) {
                                        if ("Parent".equals(userRole)) {
                                            stats.addPendingNotification("badge_perfect_week");
                                            statsRef.setValue(stats);
                                        } else {
                                            if (getContext() != null) {
                                                android.content.SharedPreferences prefs = requireContext().getSharedPreferences("badge_prefs", Context.MODE_PRIVATE);
                                                boolean notified = prefs.getBoolean("notified_badge_perfect_week", false);
                                                if (!notified) {
                                                    sendNotification("Consistent Controller");
                                                    prefs.edit().putBoolean("notified_badge_perfect_week", true).apply();
                                                }
                                            }
                                        }
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e) {}
                            });
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private boolean calculateStreak(Map<String, Integer> schedule, DataSnapshot logsSnap) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        Map<String, Integer> dailyCount = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        
        for (DataSnapshot d : logsSnap.getChildren()) {
            ControllerLogEntry entry = d.getValue(ControllerLogEntry.class);
            if (entry != null && entry.timestamp != null) {
                try {
                    Date date = fullSdf.parse(entry.timestamp);
                    if (date != null) {
                        String key = sdf.format(date);
                        dailyCount.put(key, dailyCount.getOrDefault(key, 0) + entry.doseCount);
                    }
                } catch (Exception e) {}
            }
        }
        
        Calendar iter = (Calendar) cal.clone();
        iter.add(Calendar.DAY_OF_YEAR, -6); 
        
        for (int i = 0; i < 7; i++) {
            String dateKey = sdf.format(iter.getTime());
            int dayOfWeek = iter.get(Calendar.DAY_OF_WEEK);
            String dayStr = getDayShortCode(dayOfWeek);
            
            int planned = schedule.getOrDefault(dayStr, 0);
            int actual = dailyCount.getOrDefault(dateKey, 0);
            
            if (actual < planned) {
                return false;
            }
            iter.add(Calendar.DAY_OF_YEAR, 1);
        }
        return true;
    }
    
    private String getDayShortCode(int day) {
        switch (day) {
            case Calendar.SUNDAY: return "Sun";
            case Calendar.MONDAY: return "Mon";
            case Calendar.TUESDAY: return "Tue";
            case Calendar.WEDNESDAY: return "Wed";
            case Calendar.THURSDAY: return "Thu";
            case Calendar.FRIDAY: return "Fri";
            case Calendar.SATURDAY: return "Sat";
            default: return "Mon";
        }
    }

    private void createInventoryNotificationChannel() {
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

    private void createBadgeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Badge Alerts";
            String description = "Notifications for earned badges";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(BADGE_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(String badgeName) {
        if (getContext() == null) return;
        createBadgeNotificationChannel();
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), BADGE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Badge Unlocked!")
                .setContentText("You earned the " + badgeName + " badge! Come claim it in Awards!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        try {
            notificationManager.notify(badgeName.hashCode(), builder.build());
        } catch (SecurityException e) {
            // ignore
        }
    }
}