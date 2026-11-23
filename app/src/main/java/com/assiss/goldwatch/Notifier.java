package com.assiss.goldwatch;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

final class Notifier {
    static final String CHANNEL = "gold_alerts_v2";

    private Notifier() {}

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel("gold_alerts") != null) nm.deleteNotificationChannel("gold_alerts");
        if (nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "تنبيهات سعر الذهب",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("إشعار عند وصول سعر الذهب للحد الذي حددته");
            Uri sound = Uri.parse("android.resource://" + c.getPackageName() + "/" + R.raw.gold_chime);
            ch.setSound(sound, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 250, 120, 250});
            ch.enableLights(true);
            ch.setLightColor(0xFFD4AF37);
            nm.createNotificationChannel(ch);
        }
    }

    static void show(Context c, int id, String title, String text) {
        if (Build.VERSION.SDK_INT >= 33 &&
                c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ensureChannel(c);
        PendingIntent pi = PendingIntent.getActivity(c, 0,
                new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setColor(0xFFD4AF37)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        c.getSystemService(NotificationManager.class).notify(id, n);
    }
}
