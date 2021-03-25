package com.eramint.location;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.snackbar.Snackbar;

import static com.eramint.location.ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST;
import static com.eramint.location.ForegroundOnlyLocationService.NOTIFICATION_CHANNEL_ID;
import static com.eramint.location.LocationUtil.showLocationPrompt;
import static com.eramint.location.SharedPreferenceUtil.toText;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34;
    private boolean foregroundOnlyLocationServiceBound;

    private ForegroundOnlyLocationService foregroundOnlyLocationService;

    private final ServiceConnection foregroundOnlyServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            ForegroundOnlyLocationService.LocalBinder binder = (ForegroundOnlyLocationService.LocalBinder) service;
            foregroundOnlyLocationService = binder.getService();
            foregroundOnlyLocationServiceBound = true;

            if (foregroundPermissionApproved()) {
                foregroundOnlyLocationService.subscribeToLocationUpdates();
            } else {
                requestForegroundPermissions();
            }
        }

        public void onServiceDisconnected(ComponentName name) {

            foregroundOnlyLocationService.unsubscribeToLocationUpdates();

            foregroundOnlyLocationService = null;
            foregroundOnlyLocationServiceBound = false;

        }
    };
    private ForegroundOnlyBroadcastReceiver foregroundOnlyBroadcastReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private TextView outputTextView;
    private NotificationManager notificationManager;

    private LocationManager manager;

    public LocationManager getLocationManager() {
        if (manager == null)
            manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return manager;
    }

    private ForegroundOnlyBroadcastReceiver getForegroundOnlyBroadcastReceiver() {
        if (foregroundOnlyBroadcastReceiver == null)
            this.foregroundOnlyBroadcastReceiver = new MainActivity.ForegroundOnlyBroadcastReceiver();
        return foregroundOnlyBroadcastReceiver;
    }


    public LocalBroadcastManager getLocalBroadcastManager() {
        if (localBroadcastManager == null)
            this.localBroadcastManager = LocalBroadcastManager.getInstance(this);

        return localBroadcastManager;
    }

    public NotificationManager getNotificationManager() {
        if (notificationManager == null)
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return notificationManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        outputTextView = findViewById(R.id.output_text_view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setShowBadge(true);
            notificationChannel.enableVibration(true);
            getNotificationManager().createNotificationChannel(notificationChannel);
        }

        GpsLocationReceiver br = new GpsLocationReceiver();
        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(br, filter);
        Toast.makeText(this, "NetworkType: " + SharedPreferenceUtil.getNetworkType(this), Toast.LENGTH_SHORT).show();
    }


    protected void onStart() {
        super.onStart();


        Intent serviceIntent = new Intent(this, ForegroundOnlyLocationService.class);
        this.bindService(serviceIntent, this.foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE);

    }


    protected void onResume() {
        super.onResume();

        getLocalBroadcastManager().registerReceiver(getForegroundOnlyBroadcastReceiver(), new IntentFilter(ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST));


        if (foregroundPermissionApproved() && !getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER))
            showLocationPrompt(this);
    }

    protected void onPause() {
        getLocalBroadcastManager().unregisterReceiver(getForegroundOnlyBroadcastReceiver());
        super.onPause();
    }


    protected void onStop() {
        if (this.foregroundOnlyLocationServiceBound) {
            this.unbindService(this.foregroundOnlyServiceConnection);
            this.foregroundOnlyLocationServiceBound = false;
        }
        super.onStop();
    }


    private boolean foregroundPermissionApproved() {
        return ActivityCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED;
    }

    private void requestForegroundPermissions() {
        boolean provideRationale = this.foregroundPermissionApproved();
        if (provideRationale) {
            Snackbar.make(findViewById(R.id.activity_main),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_LONG)
                    .setAction(R.string.ok, it -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE)).show();
        } else {
            Log.e("MainActivity", "Request foreground only permission");
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE);
        }

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.e("MainActivity", "onRequestPermissionResult");
        if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length == 0) {
                Log.e("MainActivity", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ForegroundOnlyLocationService service = this.foregroundOnlyLocationService;
                if (service != null) {
                    service.subscribeToLocationUpdates();
                }
            } else {
                Snackbar.make(this.findViewById(R.id.activity_main),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                ).setAction(R.string.ok, it -> openSettings()).show();

            }
        }
    }


    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity.this.startActivity(intent);
    }


    private void logResultsToScreen(String s) {
        Log.e(TAG, "logResultsToScreen: " + s);
        String value = outputTextView.getText().toString() + " \n " + s;
        outputTextView.setText(value);
    }

    private final class ForegroundOnlyBroadcastReceiver extends BroadcastReceiver {
        public ForegroundOnlyBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(ForegroundOnlyLocationService.EXTRA_LOCATION);
            if (location != null) {
                MainActivity.this.logResultsToScreen(toText(location));
            }

        }
    }
}