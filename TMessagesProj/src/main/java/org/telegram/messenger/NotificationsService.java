/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationsService extends Service {

    private static final String TAG = "NotificationsService";

    private static final String CHANNEL_ID = "main_notification_channel_id";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");

        ApplicationLoader.postInitApplication();

        Notification notification = buildStubNotification();
        startForeground(notification.hashCode(), notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        if (preferences.getBoolean("pushService", true)) {
            Intent intent = new Intent("org.telegram.start");
            sendBroadcast(intent);
        }
    }

    private Notification buildStubNotification() {
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);

        return new NotificationCompat.Builder(this, CHANNEL_ID).build();
    }

    public static void startForeground(Context context) {
        Intent intent = new Intent(context, NotificationsService.class);
        context.startForegroundService(intent);
    }
}
