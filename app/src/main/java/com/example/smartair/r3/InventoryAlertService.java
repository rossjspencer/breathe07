package com.example.smartair.r3;

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
    private static final String PREFS_NAME = "InventoryAlertPrefs";
    private static final String KEY_SENT_NOTIFICATIONS = "sent_notifications";

    private DatabaseReference userRef;
    private DatabaseReference inventoryRef;
    private Set<String> sentNotificationsCache;
    private final List<ValueEventListener> listeners = new ArrayList<>();
    private final List<DatabaseReference> refs = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sentNotificationsCache = new HashSet<>(prefs.getStringSet(KEY_SENT_NOTIFICATIONS, new HashSet<>()));
        
        checkUserRoleAndStart();
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
                    monitorLinkedChildren(snapshot.child("linkedChildren"));
                } else {
                    stopSelf();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private void monitorLinkedChildren(DataSnapshot linkedChildrenSnap) {
        for (DataSnapshot child : linkedChildrenSnap.getChildren()) {
            String childId = child.getKey();
            if (childId != null) {
                monitorInventoryForChild(childId);
            }
        }
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
        
        listeners.add(listener);
        refs.add(controllerRef);
        refs.add(rescueRef);
    }

    private void processItem(DataSnapshot child, String childId) {
        InventoryItem item = child.getValue(InventoryItem.class);
        if (item != null) {
            item.id = child.getKey();
            
            String alertKey = null;
            String title = null;
            String content = null;

            if (item.isExpired()) {
                alertKey = childId + "_" + item.id + "_expired";
                title = "Expired Medication Warning";
                content = "A child's medication " + item.name + " has expired!";
            } else if (item.percentLeft <= 0) {
                alertKey = childId + "_" + item.id + "_empty";
                title = "Empty Medication Warning";
                content = "A child's medication " + item.name + " is empty!";
            } else if (item.isLow()) {
                alertKey = childId + "_" + item.id + "_low";
                title = "Low Medication Warning";
                content = "A child's medication " + item.name + " is running low (" + item.percentLeft + "% left).";
            }

            if (alertKey != null) {
                // only send if not already in the cache
                if (!sentNotificationsCache.contains(alertKey)) {
                    sendNotification(alertKey.hashCode(), title, content);
                    sentNotificationsCache.add(alertKey);
                    
                    // Update prefs
                    SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putStringSet(KEY_SENT_NOTIFICATIONS, sentNotificationsCache).apply();
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