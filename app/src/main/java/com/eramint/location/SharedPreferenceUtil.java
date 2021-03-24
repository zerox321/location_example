package com.eramint.location;


import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.text.TextUtils;


public final class SharedPreferenceUtil {

    public static final String KEY_FOREGROUND_ENABLED = "tracking_foreground_location";

    public static String toText(Location location) {
        return location != null ? "" + '(' + location.getLatitude() + ", " + location.getLongitude() + ')' + ",   " + location.getAccuracy() + ",   Device :" + getDeviceName() + ",  V " + Build.VERSION.RELEASE : "Unknown location";
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

}


