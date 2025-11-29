package com.b07.asthmaid.r3;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.b07.asthmaid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

public class InventoryAlertService extends Service {

    private static final String TEMP_USER_ID = "testUserId";
    private static final String CHANNEL_ID = "inventory_alerts";
    private static final String PREFS_NAME = "InventoryAlertPrefs";
    private static final String KEY_SENT_NOTIFICATIONS = "sent_notifications";

    private DatabaseReference inventoryRef;
    private ValueEventListener inventoryListener;
    private Set<String> sentNotificationsCache;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        // the internet says this will make the notifications less spammy
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sentNotificationsCache = new HashSet<>(prefs.getStringSet(KEY_SENT_NOTIFICATIONS, new HashSet<>()));
        
        startMonitoring();
    }

    private void startMonitoring() {
        inventoryRef = FirebaseDatabase.getInstance().getReference("inventory");
        inventoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                checkAndSendNotifications(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        // monitor both controller and rescue nodes for the user
        inventoryRef.addValueEventListener(inventoryListener);
    }

    private void checkAndSendNotifications(DataSnapshot snapshot) {
        Set<String> currentAlerts = new HashSet<>();

        DataSnapshot controllerSnap = snapshot.child("controller").child(TEMP_USER_ID);
        for (DataSnapshot child : controllerSnap.getChildren()) {
            processItem(child, "Controller", currentAlerts);
        }

        DataSnapshot rescueSnap = snapshot.child("rescue").child(TEMP_USER_ID);
        for (DataSnapshot child : rescueSnap.getChildren()) {
            processItem(child, "Rescue", currentAlerts);
        }

        sentNotificationsCache = new HashSet<>(currentAlerts);
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_SENT_NOTIFICATIONS, sentNotificationsCache).commit();
    }

    private void processItem(DataSnapshot child, String type, Set<String> currentAlerts) {
        InventoryItem item = child.getValue(InventoryItem.class);
        if (item != null) {
            item.id = child.getKey();
            item.type = type.toLowerCase();

            String alertKey = null;
            String title = null;
            String content = null;

            if (item.percentLeft <= 0) {
                alertKey = item.id + "_empty";
                title = "Empty Medication Warning";
                content = type + " medication " + item.name + " is empty!";
            } else if (item.isLow()) {
                alertKey = item.id + "_low";
                title = "Low Medication Warning";
                content = type + " medication " + item.name + " is running low (" + item.percentLeft + "% left).";
            }

            if (alertKey != null) {
                // add to current active alerts
                currentAlerts.add(alertKey);
                
                // only send if not already in the cache
                if (!sentNotificationsCache.contains(alertKey)) {
                    sendNotification(item.id.hashCode(), title, content);
                    sentNotificationsCache.add(alertKey);
                }
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Inventory Alerts";
            String description = "Notifications for low or expired medication";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(int id, String title, String content) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true) 
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
        if (inventoryRef != null && inventoryListener != null) {
            inventoryRef.removeEventListener(inventoryListener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}