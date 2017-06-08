package ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 06/06/2017.
 */

public class Ip6Header implements L3Header {
    // https://en.wikipedia.org/wiki/IPv6_packet

    @Override
    public int getMinimumHeaderLength() {
        return 40;
    }

    @Override
    public int getTotalLength(ByteBuffer packet) {
        int len = 0;
        len |= (packet.get(4) & 0xFF) << 8;
        len |= packet.get(5) & 0xFF;
        return len;
    }
}
