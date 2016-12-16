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

    @Override
    public boolean fromLocalToRemote(ByteBuffer packet) {
        Long destinationMac = mMacResolver.getDestinationMacAddressByL3Packet(packet);

        if (destinationMac == null) {
            mMacResolver.resolveMacAndQueueL3Packet(packet);
            // unknown mac as for now.
            return false;
        }

        EthernetHeader ethernetHeader = new EthernetHeader(destinationMac,
                mMacResolver.getLocalMacAddress(), EthernetHeader.TYPE_IP);
        ethernetHeader.addToPacket(packet);
        return true;
    }

    @Override
    public boolean fromRemoteToLocal(ByteBuffer packet) {
        EthernetHeader ethernetHeader = EthernetHeader.stripFromPacket(packet);

        if (ethernetHeader.getmEtherType() == EthernetHeader.TYPE_IP)
            return true;

        if (ethernetHeader.getmEtherType() == EthernetHeader.TYPE_IPV6)
            // silently drop any IPV6 communications
            return false;

        if (ethernetHeader.getmEtherType() == EthernetHeader.TYPE_ARP) {
            try {
                mMacResolver.processIncomingArpPacket(packet);
            }
            catch (Exception e) {
                Log.i(TAG, "ARP bad incoming packet", e);
            }
            return false;
        }

        Log.i(TAG, "dropped packet with unknown etherType: " + ethernetHeader.getmEtherType());
        return false;
    }

    @Override
    public boolean nextCustomPacketToRemote(ByteBuffer buffer) {
        MacResolver.CustomPacket customPacket = mMacResolver.pollPacketFromQueue();

        if (customPacket == null)
            return false;

        ByteBufferUtils.putFromOneIntoAnother(customPacket.getPacket(), buffer);

        if (customPacket.isL3()) {
            return this.fromLocalToRemote(buffer);
        }

        return true;
    }
}
