package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by kostya on 01/02/2017.
 */

public class SocatVpnServiceReceiver extends BroadcastReceiver {

    final String configIntentStringExtraKey = "configuration";

    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = context.getPackageName();

        Intent serviceIntent = new Intent(context, SocatVpnService.class)
                .putExtra(
                        packageName + ".CONFIGURATION",
                        intent.getStringExtra(configIntentStringExtraKey)
                );

        context.startService(serviceIntent);
    }
}
