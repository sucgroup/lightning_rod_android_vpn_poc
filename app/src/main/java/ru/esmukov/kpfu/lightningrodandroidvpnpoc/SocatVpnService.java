package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.PacketFilter;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection.ServerConnection;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection.ServerConnectionFactory;

/**
 * Created by kostya on 21/10/2016.
 *
 * Based on the ToyVpn example
 * https://android.googlesource.com/platform/development/+/master/samples/ToyVpn/src/com/example/android/toyvpn/ToyVpnService.java
 */
public class SocatVpnService extends VpnService implements Handler.Callback, Runnable {
    private static final String TAG = "SocatVpnService";
    private static final int CONNECT_ATTEMPTS = 3;

    private SocatServerConnectionInfo mSocatServerConnectionInfo = null;

    private PendingIntent mConfigureIntent;
    private Handler mHandler;
    private Thread mThread;
    private ParcelFileDescriptor mInterface;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mThread != null) {
                mThread.interrupt();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(getPackageName() + ".intent.VpnDisconnect");
        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
        if (context == null) {
            throw new IllegalStateException("Application context is null");
        }

        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }
        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
        }

        if (prepare(context) != null) {
            mHandler.sendEmptyMessage(R.string.no_consent);
            return START_NOT_STICKY;
        }

        // Extract information from the intent.
        String prefix = getPackageName();
        mSocatServerConnectionInfo = new SocatServerConnectionInfo(
                intent.getStringExtra(prefix + ".CONFIGURATION")
        );
        // Start a new session by creating a new thread.
        mThread = new Thread(this, "SocatVpnServiceThread");
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public synchronized void run() {
        try {
            Log.i(TAG, "Starting");
            // We try to create the tunnel for several times. The better way
            // is to work with ConnectivityManager, such as trying only when
            // the network is avaiable. Here we just use a counter to keep
            // things simple.
            for (int attempt = 0; attempt < CONNECT_ATTEMPTS; ++attempt) {
                mHandler.sendEmptyMessage(R.string.connecting);
                // Reset the counter if we were connected.
                if (createVpnTunnel()) {
                    attempt = 0;
                }
                // Sleep for a while. This also checks if we got interrupted.
                Thread.sleep(3000);
            }
            Log.i(TAG, "Giving up");
            mHandler.sendEmptyMessage(R.string.disconnected);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Got " + e.toString());
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
            mHandler.sendEmptyMessage(R.string.disconnected);
        } finally {
            try {
                mInterface.close();
            } catch (Exception e) {
                // ignore
            }
            mInterface = null;

            Log.i(TAG, "Exiting");
        }
    }

    private boolean createVpnTunnel() throws Exception {
        ServerConnection tunnel = null;
        boolean connected = false;
        try {
            // Create a SocketChannel as the VPN tunnel.
            tunnel = ServerConnectionFactory.fromRemoteConnectionInfo(
                    mSocatServerConnectionInfo.getRemoteConnectionInfo());
            // Connect to the server.
            tunnel.connect();
            // Protect the tunnel before connecting to avoid loopback.

            if (!tunnel.protect(this)) {
                mHandler.sendEmptyMessage(R.string.no_consent);
                throw new IllegalStateException("Cannot protect the tunnel");
            }
            // For simplicity, we use the same thread for both reading and
            // writing. Here we put the tunnel into non-blocking mode.
            tunnel.configureBlocking(false);

            configureVpnInterface();
            // Now we are connected. Set the flag and show the message.
            connected = true;
            mHandler.sendEmptyMessage(R.string.connected);
            // Packets to be sent are queued in this input stream.
            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());
            // Packets received need to be written to this output stream.
            FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());
            // Allocate the buffer for a single packet.
//            ByteBuffer packet = ByteBuffer.allocate(32767);

            PacketFilter packetFilter = mSocatServerConnectionInfo.createNewPacketFilter();

            // We keep forwarding packets till something goes wrong.
            while (!Thread.interrupted()) {
                // Packet info usage (search for `struct tun_pi`):
                // http://lxr.free-electrons.com/source/drivers/net/tun.c
                // https://www.kernel.org/doc/Documentation/networking/tuntap.txt

                // Assume that we did not make any progress in this iteration.
                boolean idle = true;

                if (packetFilter.consumeLocal(in)) idle = false;
                if (packetFilter.produceRemote(tunnel)) idle = false;
                if (packetFilter.consumeRemote(tunnel)) idle = false;
                if (packetFilter.produceLocal(out)) idle = false;

                // If we are idle or waiting for the network, sleep for a
                // fraction of time to avoid busy looping.
                if (idle) {
                    // todo wait on sockets somehow instead of sleeping
                    // tunnel socket is nio, thus it's selectable
                    // but the interface socket is not.

                    Thread.sleep(30);
                }
            }
        } catch (IllegalStateException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
        } finally {
            try {
                if (tunnel != null)
                    tunnel.close();
            } catch (Exception e) {
                // ignore
            }
            try {
                if (mInterface != null)
                    mInterface.close();
            } catch (Exception e) {
                // ignore
            }
            mInterface = null;
        }
        return connected;
    }

    private void configureVpnInterface() {
        Builder builder = new Builder();
        mSocatServerConnectionInfo.applyToVpnServiceBuilder(builder);

        try {
            mInterface.close();
        } catch (Exception e) {
            // ignore
        }
        // Create a new interface using the builder and save the parameters.
        mInterface = builder.setSession(mSocatServerConnectionInfo.getVpnServiceName())
                .setConfigureIntent(mConfigureIntent)
                .establish();
        Log.i(TAG, "New interface");
    }
}
