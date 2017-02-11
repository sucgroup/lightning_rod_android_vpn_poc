package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

/**
 * Created by kostya on 11/02/2017.
 */

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Foreground service which keeps a notification in the notifications bar while listening to
 * the VpnConnect broadcasted intents.
 */
public class BroadcastsListenerService extends Service {

    final String configIntentStringExtraKey = "configuration";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
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
    };


    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(getPackageName() + ".intent.VpnConnect");
        registerReceiver(mReceiver, filter);

        makeForeground();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    private void makeForeground() {
        Intent notificationIntent = new Intent(this, SocatVpnActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(pendingIntent)
                //.setTicker(getText(R.string.ticker_text))
                .build();

        startForeground(1, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
