package ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection;

import android.net.VpnService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Created by kostya on 09/11/2016.
 */

class UdpServerConnection implements ServerConnection {
    private DatagramChannel mSocket;

    UdpServerConnection() throws IOException {
        mSocket = DatagramChannel.open();
    }

    @Override
    public boolean protect(VpnService vpnService) {
        return vpnService.protect(mSocket.socket());
    }

    @Override
    public void connect(InetSocketAddress server) throws IOException {
        mSocket.connect(server);
    }

    @Override
    public void configureBlocking(boolean blockingMode) throws IOException {
        mSocket.configureBlocking(blockingMode);
    }

    @Override
    public void write(ByteBuffer source) throws IOException {
        mSocket.write(source);
    }

    @Override
    public int read(ByteBuffer target) throws IOException {
        return mSocket.read(target);
    }

    @Override
    public void close() throws IOException {
        mSocket.close();
    }
}
