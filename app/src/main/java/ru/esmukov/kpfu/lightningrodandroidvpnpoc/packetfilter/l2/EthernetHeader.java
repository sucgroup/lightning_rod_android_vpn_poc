package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l2;

import java.nio.ByteBuffer;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.ByteBufferUtils;

/**
 * Created by kostya on 16/12/2016.
 */

class EthernetHeader {
    // http://www.iana.org/assignments/ieee-802-numbers/ieee-802-numbers.xhtml#ieee-802-numbers-1
    public static final int TYPE_ARP = 0x0806;
    public static final int TYPE_IP = 0x0800;
    public static final int TYPE_IPV6 = 0x86DD;

    private long mDestinationMac; // 6 bytes
    private long mSourceMac; // 6 bytes
    // todo 802.1Q tag
    private int mEtherType; // 2 bytes

    public EthernetHeader(long destinationMac, long sourceMac, int etherType) {
        mDestinationMac = destinationMac;
        mSourceMac = sourceMac;
        mEtherType = etherType;
    }

    public static EthernetHeader stripFromPacket(ByteBuffer packet) {
        long destinationMac = packet.getLong(0);
        // strip 2 extra bytes (8 bytes of long - 6 bytes of mac)
        destinationMac = destinationMac >> (2 * 8);

        long sourceMac = packet.getLong(6);
        sourceMac = sourceMac >> (2 * 8);

        int etherType = ((packet.get(6 + 6) & 0xFF) << 8) | (packet.get(6 + 6 + 1) & 0xFF);

        ByteBufferUtils.moveLeft(packet, 6 + 6 + 2);

        return new EthernetHeader(destinationMac, sourceMac, etherType);
    }

    public void addToPacket(ByteBuffer packet) {
        ByteBufferUtils.moveRight(packet, 6 + 6 + 2);

        packet.putShort(0, (short)(mSourceMac >> (4 * 8)));
        packet.putInt(2, (int)(mSourceMac));

        packet.putShort(6, (short)(mDestinationMac >> (4 * 8)));
        packet.putInt(8, (int)(mDestinationMac));

        packet.putShort(6 + 6, (short) mEtherType);
    }

    public long getDestinationMac() {
        return mDestinationMac;
    }

    public long getmSourceMac() {
        return mSourceMac;
    }

    public int getmEtherType() {
        return mEtherType;
    }
}
