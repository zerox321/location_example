package com.eramint.location;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import android.view.ViewGroup;
import android.view.Window;

public class NoLocationDialog {

    public static void showNoLocationDialog(LocationManager locationManager, Context context) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.no_location_dialog);
        Window window = dialog.getWindow();
        if (window != null)
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.findViewById(R.id.enableBt).setOnClickListener(v -> {
                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    dialog.dismiss();
                }

        );
        dialog.show();
    }


}
