package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 16/12/2016.
 */

/**
 * Filter (and change, if needed) a packet between remote side and a local VpnService TUN.
 */
public interface PacketFilter {
    /**
     * Outgoing L3 IP4 packet
     *
     * @param ip4Packet Local IP4 packet. This method should convert this buffer
     *                  to a format expected by the remote side
     * @return Should the packet be sent? Drop it otherwise.
     */
    boolean fromLocalToRemote(ByteBuffer ip4Packet);

    /**
     * Incoming packet
     *
     * @param remotePacket This method should convert this raw packet received
     *                     from the remote side to a local IP4 packet.
     * @return Should the packet be accepted? Drop it otherwise.
     */
    boolean fromRemoteToLocal(ByteBuffer remotePacket);

    /**
     * Initiate custom outgoing packets
     *
     * @param remotePacket
     * @return Is a packet to be sent put to the `remotePacket` buffer
     */
    boolean nextCustomPacketToRemote(ByteBuffer remotePacket);
}
