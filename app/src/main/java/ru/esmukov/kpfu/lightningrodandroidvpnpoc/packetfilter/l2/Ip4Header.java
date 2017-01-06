package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l2;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 06/01/2017.
 */

class Ip4Header {

    static int getDestinationIp(ByteBuffer packet) {
        return packet.getInt(16);
    }

    static int getSourceIp(ByteBuffer packet) {
        return packet.getInt(12);
    }
}
