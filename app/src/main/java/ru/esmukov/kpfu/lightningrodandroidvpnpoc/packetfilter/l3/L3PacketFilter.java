package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l3;

import android.util.Log;

import java.nio.ByteBuffer;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.PacketFilter;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.PacketInfo;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l2.EthernetHeader;

/**
 * Created by kostya on 16/12/2016.
 */

public class L3PacketFilter implements PacketFilter {
    private static final String TAG = "L3PacketFilter";

    private boolean mPacketInfo;

    public L3PacketFilter(boolean packetInfo) {
        this.mPacketInfo = packetInfo;
    }

    @Override
    public boolean fromLocalToRemote(ByteBuffer ip4Packet) {
        if (mPacketInfo) {
            PacketInfo.prepend(ip4Packet, EthernetHeader.TYPE_IP);
        }
        return true;
    }

    @Override
    public boolean fromRemoteToLocal(ByteBuffer remotePacket) {
        if (mPacketInfo) {
            try {
                int proto = PacketInfo.strip(remotePacket).getProto();

                if (proto != EthernetHeader.TYPE_IP) {
                    // skin non-IP packets
                    Log.w(TAG, "received non-IP proto: " + proto);
                    return false;
                }
            } catch (Exception e) {
                Log.i(TAG, "Packet info exception", e);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean nextCustomPacketToRemote(ByteBuffer remotePacket) {
        return false;
    }
}
