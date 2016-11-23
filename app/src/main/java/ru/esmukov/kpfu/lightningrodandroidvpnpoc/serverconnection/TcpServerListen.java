package ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection;

import android.net.VpnService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by kostya on 23/11/2016.
 */

public class TcpServerListen implements ServerConnection {
    private ServerSocketChannel mServerSocket;
    private SocketChannel mSocket;
    private InetSocketAddress mListen;

    TcpServerListen(InetSocketAddress listen) throws IOException {
        mServerSocket = ServerSocketChannel.open();
        mListen = listen;
    }

    @Override
    public boolean protect(VpnService vpnService) {
        return vpnService.protect(mSocket.socket());
    }

    @Override
    public void connect() throws IOException {
        mServerSocket.socket().bind(mListen);

        mSocket = mServerSocket.accept();
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
        mServerSocket.close();
    }
}
