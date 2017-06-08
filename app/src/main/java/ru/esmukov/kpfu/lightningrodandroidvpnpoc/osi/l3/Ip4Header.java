package ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 06/01/2017.
 */

public class Ip4Header implements L3Header {
    // https://en.wikipedia.org/wiki/IPv4#Packet_structure

    public static int getDestinationIp(ByteBuffer packet) {
        return packet.getInt(16);
    }

    public static int getSourceIp(ByteBuffer packet) {
        return packet.getInt(12);
    }

    @Override
    public int getTotalLength(ByteBuffer packet) {
        int len = 0;
        len |= (packet.get(2) & 0xFF) << 8;
        len |= packet.get(3) & 0xFF;
        return len;
    }

    @Override
    public int getMinimumHeaderLength() {
        return 20;
    }
}
