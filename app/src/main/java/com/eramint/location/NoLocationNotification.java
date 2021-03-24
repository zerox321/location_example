package com.eramint.location;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import static com.eramint.location.ForegroundOnlyLocationService.NOTIFICATION_CHANNEL_ID;

public class NoLocationNotification {
    private static final int No_Location_NOTIFICATION_ID = 12345678;

    public static void dismissNoLocationNotification(NotificationManager notificationManager) {
        notificationManager.cancel(No_Location_NOTIFICATION_ID);
    }

    public static void showNoLocationNotification(NotificationManager notificationManager, Context context) {


        String titleText = context.getString(R.string.location_disabled);
        String mainNotificationText = context.getString(R.string.location_disabled_content);

        NotificationCompat.BigTextStyle bigTextStyle = (new NotificationCompat.BigTextStyle()).bigText(mainNotificationText).setBigContentTitle(titleText);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notificationCompatBuilder = new NotificationCompat.Builder(context.getApplicationContext(), NOTIFICATION_CHANNEL_ID);
        notificationCompatBuilder.setStyle(bigTextStyle)
                .setContentTitle(titleText)
                .setContentText(mainNotificationText)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        notificationManager.notify(No_Location_NOTIFICATION_ID, notificationCompatBuilder.build());
    }
}

