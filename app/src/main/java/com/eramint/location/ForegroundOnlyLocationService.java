package com.eramint.location;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationCompat.Builder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.TimeUnit;

import static com.eramint.location.SharedPreferenceUtil.toText;


public final class ForegroundOnlyLocationService extends Service {

    public static final String NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01";
    private static final String TAG = "ForegroundService";
    private static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    public static final String EXTRA_LOCATION = PACKAGE_NAME + ".extra.LOCATION";
    public static final String ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST = PACKAGE_NAME + ".action.FOREGROUND_ONLY_LOCATION_BROADCAST";
    private static final String EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION = PACKAGE_NAME + ".extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION";
    private static final int NOTIFICATION_ID = 12345678;

    private final Long interval = 2L;

    private final ForegroundOnlyLocationService.LocalBinder localBinder = new ForegroundOnlyLocationService.LocalBinder();
    private final LocationRequest locationRequest = LocationRequest.create()
            .setInterval(TimeUnit.SECONDS.toMillis(interval))
            .setFastestInterval(TimeUnit.SECONDS.toMillis(interval))
            .setMaxWaitTime(TimeUnit.SECONDS.toMillis(interval))
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    private boolean configurationChange = false;
    private boolean serviceRunningInForeground = false;

    private NotificationManager notificationManager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Location currentLocation;


    public static NotificationManager getForegroundOnlyLocationService(ForegroundOnlyLocationService foregroundOnlyLocationService) {
        return foregroundOnlyLocationService.notificationManager;

    }

    public void onCreate() {
        Log.e(TAG, "onCreate()");
        this.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        this.locationCallback = new LocationCallback() {
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location location = locationResult.getLastLocation();

                Log.e(TAG, "onLocationResult:  lat : " + location.getLatitude() + " , lon : " + location.getLongitude());

                ForegroundOnlyLocationService.this.currentLocation = location;
                Intent intent = new Intent(ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST);
                intent.putExtra(EXTRA_LOCATION, ForegroundOnlyLocationService.this.currentLocation);
                LocalBroadcastManager.getInstance(ForegroundOnlyLocationService.this.getApplicationContext()).sendBroadcast(intent);

                if (ForegroundOnlyLocationService.this.serviceRunningInForeground) {
                    ForegroundOnlyLocationService.getForegroundOnlyLocationService(ForegroundOnlyLocationService.this).notify(NOTIFICATION_ID, ForegroundOnlyLocationService.this.generateNotification(ForegroundOnlyLocationService.this.currentLocation));
                }

            }
        };
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand()");
        boolean cancelLocationTrackingFromNotification = intent.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false);
        if (cancelLocationTrackingFromNotification) {
            this.unsubscribeToLocationUpdates();
            this.stopSelf();
        }

        return START_NOT_STICKY;
    }

    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind()");
        this.stopForeground(true);
        this.serviceRunningInForeground = false;
        this.configurationChange = false;
        return this.localBinder;
    }

    public void onRebind(Intent intent) {
        Log.e(TAG, "onRebind()");
        this.stopForeground(true);
        this.serviceRunningInForeground = false;
        this.configurationChange = false;
        super.onRebind(intent);
    }

    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind()");
        if (!this.configurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {
            Log.e(TAG, "Start foreground service");
            Notification notification = this.generateNotification(this.currentLocation);
            this.startForeground(NOTIFICATION_ID, notification);
            this.serviceRunningInForeground = true;
        }

        return true;
    }

    public void onDestroy() {
        Log.e(TAG, "onDestroy()");
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.configurationChange = true;
    }

    public final void subscribeToLocationUpdates() {
        Log.e(TAG, "subscribeToLocationUpdates()");
        SharedPreferenceUtil.saveLocationTrackingPref(this, true);
        this.startService(new Intent(this.getApplicationContext(), ForegroundOnlyLocationService.class));

        try {
            FusedLocationProviderClient fusedLocationProviderClient = this.fusedLocationProviderClient;
            if (fusedLocationProviderClient == null) return;

            LocationRequest locationRequest = this.locationRequest;

            LocationCallback locationCallback = this.locationCallback;
            if (locationCallback == null) return;

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException var2) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, false);
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. " + var2);
        }

    }

    public void unsubscribeToLocationUpdates() {
        Log.e(TAG, "unsubscribeToLocationUpdates()");

        try {
            FusedLocationProviderClient fusedLocationProviderClient = this.fusedLocationProviderClient;
            if (fusedLocationProviderClient == null) return;

            LocationCallback locationCallback = this.locationCallback;
            if (locationCallback == null) return;

            Task removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            removeTask.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.e(TAG, "Location Callback removed.");
                    ForegroundOnlyLocationService.this.stopSelf();
                } else {
                    Log.e(TAG, "Failed to remove Location Callback.");
                }

            });
            SharedPreferenceUtil.saveLocationTrackingPref(this, false);
        } catch (SecurityException var2) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, true);
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. " + var2);
        }

    }

    private Notification generateNotification(Location location) {
        String titleText = getString(R.string.app_name);
        String mainNotificationText;
        if (location != null) mainNotificationText = toText(location);
        else mainNotificationText = getString(R.string.no_location_text);


        BigTextStyle bigTextStyle = (new BigTextStyle()).bigText(mainNotificationText).setBigContentTitle(titleText);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Builder notificationCompatBuilder = new Builder(this.getApplicationContext(), NOTIFICATION_CHANNEL_ID);
        return notificationCompatBuilder.setStyle(bigTextStyle)
                .setContentTitle(titleText)
                .setContentText(mainNotificationText)
                .setContentIntent(activityPendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }

    public final class LocalBinder extends Binder {
        public final ForegroundOnlyLocationService getService() {
            return ForegroundOnlyLocationService.this;
        }
    }


}

