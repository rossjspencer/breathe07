package com.example.smartair;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.HashSet;
import java.util.Set;

public class AlertHelper {
    private static final String CHANNEL_ID = "smartair_alerts";
    private final Context context;
    private final NotificationManagerCompat managerCompat;
    private final Set<String> firedKeys = new HashSet<>();
    private int notificationId = 2000;

    public AlertHelper(Context context) {
        this.context = context;
        this.managerCompat = NotificationManagerCompat.from(context);
        ensureChannel();
    }

    public void maybeNotify(String key, String title, String message) {
        if (firedKeys.contains(key)) return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        managerCompat.notify(notificationId++, builder.build());
        firedKeys.add(key);
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SMART AIR Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Safety and adherence alerts for SMART AIR parents");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }
}
