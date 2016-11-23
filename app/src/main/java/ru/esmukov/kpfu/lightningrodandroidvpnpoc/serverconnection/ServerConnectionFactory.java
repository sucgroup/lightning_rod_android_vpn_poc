package ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection;

import java.io.IOException;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.SocatServerConnectionInfo;

/**
 * Created by kostya on 09/11/2016.
 */

public class ServerConnectionFactory {

    public static ServerConnection fromRemoteConnectionInfo(
            SocatServerConnectionInfo.RemoteConnectionInfo remoteConnectionInfo) throws IOException {

        switch (remoteConnectionInfo.getSocatProtocol()) {
            case TCP:
                if (remoteConnectionInfo.isConnectSocket())
                    return new TcpServerConnect(remoteConnectionInfo.getInetSocketAddress());
                else
                    return new TcpServerListen(remoteConnectionInfo.getInetSocketAddress());
            case UDP:
                if (remoteConnectionInfo.isConnectSocket())
                    return new UdpServerConnect(remoteConnectionInfo.getInetSocketAddress());
            default:
                throw new IllegalArgumentException("Unknown protocol");
        }
    }
}
