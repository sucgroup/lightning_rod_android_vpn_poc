package ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection;

import java.io.IOException;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.SocatProtocol;

/**
 * Created by kostya on 09/11/2016.
 */

public class ServerConnectionFactory {

    public static ServerConnection fromProtocol(SocatProtocol protocol) throws IOException {
        switch (protocol) {
            case TCP:
                return new TcpServerConnection();
            case UDP:
                return new UdpServerConnection();
            default:
                throw new IllegalArgumentException("Unknown protocol");
        }
    }
}
