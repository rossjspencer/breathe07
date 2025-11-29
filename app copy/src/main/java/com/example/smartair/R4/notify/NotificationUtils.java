package com.example.smartair.R4.notify;

import android.app.*;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationUtils {
    private static final String CHANNEL_ID = "r4_alerts";

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "R4 Alerts", NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(ch);
            }
        }
    }

    public static void show(Context ctx, String title, String text, int id) {
        ensureChannel(ctx);
        Notification n = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, n);
    }
}
