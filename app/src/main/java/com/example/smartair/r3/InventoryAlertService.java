package com.example.smartair.r3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.smartair.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InventoryAlertService extends Service {

    private static final String CHANNEL_ID = "inventory_alerts";
    private static final String SERVICE_CHANNEL_ID = "inventory_service_channel";
    private static final int FOREGROUND_ID = 1001;
    
    private static final String PREFS_NAME = "InventoryAlertPrefs";
    private static final String KEY_SENT_NOTIFICATIONS = "sent_notifications";

    private DatabaseReference userRef;
    private DatabaseReference inventoryRef;
    private Set<String> sentNotificationsCache;
    
    private final Set<String> monitoredChildren = new HashSet<>();
    private final List<ValueEventListener> listeners = new ArrayList<>();
    private final List<DatabaseReference> refs = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sentNotificationsCache = new HashSet<>(prefs.getStringSet(KEY_SENT_NOTIFICATIONS, new HashSet<>()));
        
        startForegroundService();
        checkUserRoleAndStart();
    }
    
    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
                .setContentTitle("SmartAir Inventory Monitor")
                .setContentText("Monitoring medication levels...")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // appease android security
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                 startForeground(FOREGROUND_ID, notification, 1);
            } else {
                 startForeground(FOREGROUND_ID, notification);
            }
        } catch (Exception e) {
            try {
                startForeground(FOREGROUND_ID, notification);
            } catch (Exception ignored) {

            }
        }
    }

    private void checkUserRoleAndStart() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            stopSelf();
            return;
        }
        
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        inventoryRef = FirebaseDatabase.getInstance().getReference("inventory");
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.child("role").getValue(String.class);
                if ("Parent".equals(role)) {
                    // listen for linked children changes continuously
                    monitorLinkedChildren(uid);
                } else {
                    stopSelf();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private void monitorLinkedChildren(String parentUid) {
        userRef.child("linkedChildren").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String childId = child.getKey();
                    if (childId != null && !monitoredChildren.contains(childId)) {
                        monitorInventoryForChild(childId);
                        monitoredChildren.add(childId);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private void monitorInventoryForChild(String childId) {
        DatabaseReference controllerRef = inventoryRef.child("controller").child(childId);
        DatabaseReference rescueRef = inventoryRef.child("rescue").child(childId);
        
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot itemSnap : snapshot.getChildren()) {
                    processItem(itemSnap, childId);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        
        controllerRef.addValueEventListener(listener);
        rescueRef.addValueEventListener(listener);
        
        // keep track to clean up
        listeners.add(listener);
        listeners.add(listener); // added twice because attached to two refs
        refs.add(controllerRef);
        refs.add(rescueRef);
    }

    private void processItem(DataSnapshot child, String childId) {
        InventoryItem item = child.getValue(InventoryItem.class);
        if (item != null) {
            item.id = child.getKey();
            item.updatePercentLeft(); 
            
            // 1. expired
            String expiredKey = childId + "_" + item.id + "_expired";
            if (item.isExpired()) {
                if (!sentNotificationsCache.contains(expiredKey)) {
                    sendNotification(expiredKey.hashCode(), "Expired Medication Warning", "A child's medication " + item.name + " has expired!");
                    updateCache(expiredKey, true);
                }
            } else {
                updateCache(expiredKey, false);
            }

            // 2. empty
            String emptyKey = childId + "_" + item.id + "_empty";
            if (item.percentLeft <= 0) {
                if (!sentNotificationsCache.contains(emptyKey)) {
                     sendNotification(emptyKey.hashCode(), "Empty Medication Warning", "A child's medication " + item.name + " is empty!");
                     updateCache(emptyKey, true);
                }
            } else {
                updateCache(emptyKey, false);
            }

            // 3. low (only if not empty)
            String lowKey = childId + "_" + item.id + "_low";
            if (item.isLow() && item.percentLeft > 0) {
                if (!sentNotificationsCache.contains(lowKey)) {
                    sendNotification(lowKey.hashCode(), "Low Medication Warning", "A child's medication " + item.name + " is running low (" + item.percentLeft + "% left).");
                    updateCache(lowKey, true);
                }
            } else {
                if (!item.isLow()) {
                    updateCache(lowKey, false);
                }
            }
        }
    }

    private void updateCache(String key, boolean add) {
        boolean changed = false;
        if (add) {
            if (!sentNotificationsCache.contains(key)) {
                sentNotificationsCache.add(key);
                changed = true;
            }
        } else {
            if (sentNotificationsCache.contains(key)) {
                sentNotificationsCache.remove(key);
                changed = true;
            }
        }
        
        if (changed) {
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putStringSet(KEY_SENT_NOTIFICATIONS, sentNotificationsCache).apply();
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // channel for Alerts
            CharSequence name = "Inventory Alerts";
            String description = "Notifications for low or expired medication";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
            
            // channel for Service
            NotificationChannel serviceChannel = new NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    "Inventory Monitor Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }

    private void sendNotification(int id, String title, String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(id, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < refs.size(); i++) {
            refs.get(i).removeEventListener(listeners.get(i));
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}