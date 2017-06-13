package ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 06/06/2017.
 */

public class Ip6Header implements L3Header {
    // https://en.wikipedia.org/wiki/IPv6_packet

    private static final int IP6_HEADER_LENGTH = 40;

    @Override
    public int getMinimumHeaderLength() {
        return IP6_HEADER_LENGTH;
    }

    @Override
    public int getTotalLength(ByteBuffer packet) {
        int len = 0;
        len |= (packet.get(4) & 0xFF) << 8;
        len |= packet.get(5) & 0xFF;
        return len + IP6_HEADER_LENGTH;
    }

    @Override
    public boolean isMatch(ByteBuffer packet) {
        return packet.limit() >= 1 && (packet.get(0) >> 4) == 6;
    }
}
