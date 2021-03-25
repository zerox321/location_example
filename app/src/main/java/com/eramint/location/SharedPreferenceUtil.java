package com.eramint.location;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.util.Date;
import java.util.TimeZone;


public final class SharedPreferenceUtil {

    public static final String KEY_FOREGROUND_ENABLED = "tracking_foreground_location";

    public static String getTimeZone() {
        TimeZone timeZone = TimeZone.getDefault();
        boolean inDaylightTime = timeZone.inDaylightTime(new Date());
        return timeZone.getID() + " " + timeZone.getDisplayName(inDaylightTime, TimeZone.LONG);
    }

    private static boolean getIsEnableBluetooth() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        return btAdapter != null && btAdapter.isEnabled();
    }

    public static String toText(Location location) {
        return location != null ?
                "" + '(' + location.getLatitude() + ", " + location.getLongitude() + ')' +
                        ",   " + location.getAccuracy() +
                        ",\n   Device :" + getDeviceName() +
                        ",  VERSION " + Build.VERSION.RELEASE +
                        " , IsEnableBluetooth " + getIsEnableBluetooth() +
                        " , zone " + getTimeZone() + "\n\n"
                : "Unknown location";
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }
        return phrase.toString();
    }

    public static boolean getLocationTrackingPref(Context context) {
        return context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE).getBoolean(KEY_FOREGROUND_ENABLED, false);
    }

    public static void saveLocationTrackingPref(Context context, boolean requestingLocationUpdates) {
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_FOREGROUND_ENABLED, requestingLocationUpdates);
        editor.apply();
    }

    public static String getNetworkType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network nw = connectivityManager.getActiveNetwork();
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            if (actNw == null)
                return "Not Connected"; // not connected
            if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "WIFI";
            else if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "4G";
            else if (actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "ETHERNET";
            else return "?";

        } else {
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            if (info == null || !info.isConnected())
                return "Not Connected"; // not connected
            if (info.getType() == ConnectivityManager.TYPE_WIFI)
                return "WIFI";
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                int networkType = info.getSubtype();
                switch (networkType) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_IDEN:     // api< 8: replace by 11
                    case TelephonyManager.NETWORK_TYPE_GSM:      // api<25: replace by 16
                        return "2G";
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:   // api< 9: replace by 12
                    case TelephonyManager.NETWORK_TYPE_EHRPD:    // api<11: replace by 14
                    case TelephonyManager.NETWORK_TYPE_HSPAP:    // api<13: replace by 15
                    case TelephonyManager.NETWORK_TYPE_TD_SCDMA: // api<25: replace by 17
                        return "3G";
                    case TelephonyManager.NETWORK_TYPE_LTE:      // api<11: replace by 13
                    case TelephonyManager.NETWORK_TYPE_IWLAN:    // api<25: replace by 18
                    case 19: // LTE_CA
                        return "4G";
                    case TelephonyManager.NETWORK_TYPE_NR:       // api<29: replace by 20
                        return "5G";
                    default:
                        return "?";
                }
            }
        }
        return "?";
    }

}


