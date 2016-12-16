package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 16/12/2016.
 */

/**
 * Filter (and change, if needed) a package between remote side and a local VpnService TUN.
 */
public interface PacketFilter {
    boolean fromLocalToRemote(ByteBuffer packet);

    boolean fromRemoteToLocal(ByteBuffer packet);

    boolean nextCustomPacketToRemote(ByteBuffer buffer);
}
