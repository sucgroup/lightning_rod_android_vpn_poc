package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }
        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
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
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
        } finally {
            try {
                mInterface.close();
            } catch (Exception e) {
                // ignore
            }
            mInterface = null;
            mHandler.sendEmptyMessage(R.string.disconnected);
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
            // Protect the tunnel before connecting to avoid loopback.
            if (!tunnel.protect(this)) {
                throw new IllegalStateException("Cannot protect the tunnel");
            }
            // Connect to the server.
            tunnel.connect();
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
            ByteBuffer packet = ByteBuffer.allocate(32767);
            // We keep forwarding packets till something goes wrong.
            while (!Thread.interrupted()) {
                // todo handle PI
                // Assume that we did not make any progress in this iteration.
                boolean idle = true;
                // Read the outgoing packet from the input stream.
                int length = in.read(packet.array());
                if (length > 0) {
                    // Write the outgoing packet to the tunnel.
                    packet.limit(length);
                    tunnel.write(packet);
                    packet.clear();
                    // There might be more outgoing packets.
                    idle = false;
                }
                // Read the incoming packet from the tunnel.
                length = tunnel.read(packet);
                if (length > 0) {
                    // Write the incoming packet to the output stream.
                    out.write(packet.array(), 0, length);
                    packet.clear();
                    // There might be more incoming packets.
                    idle = false;
                }
                // If we are idle or waiting for the network, sleep for a
                // fraction of time to avoid busy looping.
                if (idle) {
                    // todo wait on sockets somehow instead of sleeping
                    // tunnel socket is nio, thus it's selectable
                    // but the interface socket is not.

                    Thread.sleep(30);
                }
            }
        } catch (InterruptedException e) {
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
