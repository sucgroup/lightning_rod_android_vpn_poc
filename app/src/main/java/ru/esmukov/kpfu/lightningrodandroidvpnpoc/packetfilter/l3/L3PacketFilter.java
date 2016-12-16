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
    public boolean fromLocalToRemote(ByteBuffer packet) {
        if (mPacketInfo) {
            return L3PacketInfo.fromLocalToRemote(packet);
        }
        return true;
    }

    @Override
    public boolean fromRemoteToLocal(ByteBuffer packet) {
        if (mPacketInfo) {
            return L3PacketInfo.fromRemoteToLocal(packet);
        }
        return true;
    }

    @Override
    public boolean nextCustomPacketToRemote(ByteBuffer buffer) {
        return false;
    }
}
