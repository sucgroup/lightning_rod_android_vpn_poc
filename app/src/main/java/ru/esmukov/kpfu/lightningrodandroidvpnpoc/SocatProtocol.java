package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

/**
 * Created by kostya on 09/11/2016.
 */

public enum SocatProtocol {
    TCP, UDP;

    public static SocatProtocol fromString(String protocol) {
        if ("tcp".equals(protocol.toLowerCase()))
            return TCP;

        if ("udp".equals(protocol.toLowerCase()))
            return UDP;

        throw new IllegalArgumentException("Unknown protocol. Must be one of: tcp, udp.");
    }
}
