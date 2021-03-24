package com.eramint.location;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

import static com.eramint.location.NoLocationNotification.dismissNoLocationNotification;
import static com.eramint.location.NoLocationNotification.showNoLocationNotification;

public class GpsLocationReceiver extends BroadcastReceiver {
    private final static String TAG = "LocationProviderChanged";

    boolean isGpsEnabled;
    boolean isNetworkEnabled;
    NotificationManager notificationManager;
    public NotificationManager getNotificationManager(Context context) {
        if (notificationManager == null)
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager;
    }



    public GpsLocationReceiver() {
    }


    // START OF onReceive
    @Override
    public void onReceive(Context context, Intent intent) {


        // PRIMARY RECEIVER
        if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {

            Log.e(TAG, "Location Providers Changed");

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Log.e(TAG, "change: isGpsEnabled : " + isGpsEnabled + " isNetworkEnabled : " + isNetworkEnabled);

            if (!isGpsEnabled) showNoLocationNotification(getNotificationManager(context), context);
            else dismissNoLocationNotification(getNotificationManager(context));


        }


        // BOOT COMPLETED (REPLICA OF PRIMARY RECEIVER CODE FOR WHEN BOOT_COMPLETED)
        if (intent.getAction().matches("android.intent.action.BOOT_COMPLETED")) {

            Log.i(TAG, "Location Providers Changed Boot");

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Log.e(TAG, "boot: isGpsEnabled : " + isGpsEnabled + " isNetworkEnabled : " + isNetworkEnabled);


            if (!isGpsEnabled) showNoLocationNotification(getNotificationManager(context), context);
            else dismissNoLocationNotification(getNotificationManager(context));


        }


    }
}