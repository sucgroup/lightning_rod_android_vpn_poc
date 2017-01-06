package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l3;

import java.nio.ByteBuffer;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.PacketFilter;

/**
 * Created by kostya on 16/12/2016.
 */

public class L3PacketFilter implements PacketFilter {
    private boolean mPacketInfo;

    public L3PacketFilter(boolean packetInfo) {
        this.mPacketInfo = packetInfo;
    }

    @Override
    public boolean fromLocalToRemote(ByteBuffer ip4Packet) {
        if (mPacketInfo) {
            return L3PacketInfo.fromLocalToRemote(ip4Packet);
        }
        return true;
    }

    @Override
    public boolean fromRemoteToLocal(ByteBuffer remotePacket) {
        if (mPacketInfo) {
            return L3PacketInfo.fromRemoteToLocal(remotePacket);
        }
        return true;
    }

    @Override
    public boolean nextCustomPacketToRemote(ByteBuffer remotePacket) {
        return false;
    }
}
