package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l2;

import android.util.Log;

import java.nio.ByteBuffer;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.ByteBufferUtils;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.PacketFilter;

/**
 * Created by kostya on 16/12/2016.
 */

public class L2ToL3PacketFilter implements PacketFilter {
    private static final String TAG = "L2ToL3PacketFilter";

    private MacResolver mMacResolver = new MacResolver(
            LocalMacAddressGenerator.generateRandomLocallyAdministeredMacAddress());

    public L2ToL3PacketFilter(boolean packetInfo) {
        // todo !!! packet info
        // 0x0000 + ethertype
    }

    /**
     * Prepend L2 headers to an outgoing L3 IP4 packet
     *
     * @param ip4Packet Local IP4 packet
     * @return Should the packet be sent? Drop it otherwise.
     */
    @Override
    public boolean fromLocalToRemote(ByteBuffer ip4Packet) {
        Long destinationMac = mMacResolver.getDestinationMacAddressByL3IpPacket(ip4Packet);

        if (destinationMac == null) {
            mMacResolver.resolveMacAndQueueL3IpPacket(ip4Packet);
            // unknown destination mac as for now.
            return false;
        }

        EthernetHeader ethernetHeader = new EthernetHeader(destinationMac,
                mMacResolver.getLocalMacAddress(), EthernetHeader.TYPE_IP);
        ethernetHeader.addToPacket(ip4Packet);

        return true;
    }

    /**
     * Strip L2 headers and accept L3 IP4 packets only
     *
     * @param remotePacket Raw L2 packet
     * @return Should the packet be accepted? Drop it otherwise.
     */
    @Override
    public boolean fromRemoteToLocal(ByteBuffer remotePacket) {
        EthernetHeader ethernetHeader;
        try {
            ethernetHeader = EthernetHeader.stripFromFrame(remotePacket);
        } catch (Exception e) {
            Log.i(TAG, "ETHERNET bad incoming packet", e);
            return false;
        }

        if (ethernetHeader.getEtherType() == EthernetHeader.TYPE_IP)
            return mMacResolver.shouldFrameBeAccepted(ethernetHeader.getDestinationMac());

        if (ethernetHeader.getEtherType() == EthernetHeader.TYPE_IPV6)
            // silently drop any IPV6 communications
            return false;

        if (ethernetHeader.getEtherType() == EthernetHeader.TYPE_ARP) {
            try {
                mMacResolver.processIncomingArpPacket(remotePacket);
            } catch (Exception e) {
                Log.i(TAG, "ARP bad incoming packet", e);
            }
            return false;
        }

        Log.i(TAG, "dropped packet with unknown etherType: " + ethernetHeader.getEtherType());
        return false;
    }

    /**
     * Send pending packet (i.e. outgoing ARP request,
     * IP4 packet with recently resolved destination MAC)
     *
     * @param remotePacket Any buffer
     * @return Is a packet to be sent put to the `remotePacket` buffer
     */
    @Override
    public boolean nextCustomPacketToRemote(ByteBuffer remotePacket) {
        MacResolver.CustomPacket customPacket = mMacResolver.pollPacketFromQueue();

        if (customPacket == null)
            return false;

        ByteBufferUtils.copy(customPacket.getPacket(), remotePacket);

        if (customPacket.isL3()) {
            return this.fromLocalToRemote(remotePacket);
        }

        return true;
    }
}
