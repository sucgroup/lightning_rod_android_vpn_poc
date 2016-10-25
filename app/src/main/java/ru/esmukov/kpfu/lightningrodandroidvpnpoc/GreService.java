package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

/**
 * Created by kostya on 13/10/2016.
 *
 * http://www.thegeekstuff.com/2014/06/android-vpn-service/
 *
 */
public class GreService extends VpnService {

    private Thread thread;
    private ParcelFileDescriptor greInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    greInterface = new Builder()
                            .setSession("GreServiceService")
                            .addAddress("192.168.0.1", 24)
                            .addDnsServer("8.8.8.8")
                            .addRoute("0.0.0.0", 0)
                            .establish();

                    FileInputStream in = new FileInputStream(
                            greInterface.getFileDescriptor());

                    FileOutputStream out = new FileOutputStream(
                            greInterface.getFileDescriptor());

                    DatagramChannel tunnel = DatagramChannel.open();

                    tunnel.connect(new InetSocketAddress("127.0.0.1", 8087));

                    protect(tunnel.socket());

                    while (!Thread.interrupted()) {
                        Thread.sleep(100);
                    }

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    try {
                        if (greInterface != null) {
                            greInterface.close();
                            greInterface = null;
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }, "GreServiceRunnable");

        thread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (thread != null) {
            thread.interrupt();
        }
        super.onDestroy();
    }

}
