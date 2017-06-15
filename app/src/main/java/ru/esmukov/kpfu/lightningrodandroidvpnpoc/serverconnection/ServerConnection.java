package ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection;

import android.net.VpnService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Created by kostya on 09/11/2016.
 */

public interface ServerConnection {
    boolean protect(VpnService vpnService);

    void connect() throws IOException;

    void configureBlocking(boolean blockingMode) throws IOException;

    int write(ByteBuffer source) throws IOException;

    int read(ByteBuffer target) throws IOException;

    void close() throws IOException;
}
