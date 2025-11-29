package com.example.smartair.R4.notify;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationUtils {
    private static final String CHANNEL_ID = "r4_alerts";

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID, "R4 Alerts", NotificationManager.IMPORTANCE_HIGH);
                ch.enableLights(true);
                ch.setLightColor(Color.RED);
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
            }
        }
    }

    /** Optional: opens your app when tapping the notification */
    public static PendingIntent buildContentIntent(Context ctx, Intent intent, int reqCode) {
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                ctx, reqCode, intent,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void show(Context ctx, String title, String text, int id, PendingIntent pi) {
        ensureChannel(ctx);
        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        if (pi != null) b.setContentIntent(pi);

        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, b.build());
    }
}
