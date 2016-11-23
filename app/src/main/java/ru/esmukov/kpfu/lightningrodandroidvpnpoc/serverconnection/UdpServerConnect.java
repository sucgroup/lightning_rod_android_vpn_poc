package ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection;

import android.net.VpnService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Created by kostya on 09/11/2016.
 */

class UdpServerConnect implements ServerConnection {
    private DatagramChannel mSocket;
    private InetSocketAddress mServer;

    UdpServerConnect(InetSocketAddress server) throws IOException {
        mSocket = DatagramChannel.open();
        mServer = server;
    }

    @Override
    public boolean protect(VpnService vpnService) {
        return vpnService.protect(mSocket.socket());
    }

    @Override
    public void connect() throws IOException {
        mSocket.connect(mServer);
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
