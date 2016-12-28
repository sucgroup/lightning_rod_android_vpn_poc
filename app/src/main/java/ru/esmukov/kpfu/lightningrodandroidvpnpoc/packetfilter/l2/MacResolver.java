package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l2;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by kostya on 16/12/2016.
 */

// non thread safe
class MacResolver {

    private long mLocalMacAddress;
    private Map<Integer, Long> mIpToMac = new HashMap<>();
    private Deque<CustomPacket> mPacketDeque = new LinkedList<>();
    private Map<Integer, Deque<CustomPacket>> mIpToPacketsWaitingForMacResolution = new HashMap<>();

    public MacResolver(long localMacAddress) {
        mLocalMacAddress = localMacAddress;
    }

    public long getLocalMacAddress() {
        return mLocalMacAddress;
    }


    public Long getDestinationMacAddressByL3Packet(ByteBuffer packet) {
        return mIpToMac.get(getDestinationIpFromL3Packet(packet));
    }

    public void resolveMacAndQueueL3Packet(ByteBuffer packet) {
        int destinationIp = getDestinationIpFromL3Packet(packet);

        mPacketDeque.add(CustomPacket.fromL2Packet(
                Arp.createRequest(getLocalMacAddress(), getSourceIpFromL3Packet(packet), destinationIp)));

        if (!mIpToPacketsWaitingForMacResolution.containsKey(destinationIp)) {
            mIpToPacketsWaitingForMacResolution.put(destinationIp, new LinkedList<CustomPacket>());
        }

        mIpToPacketsWaitingForMacResolution.get(destinationIp).add(
                CustomPacket.fromL3Packet(packet.duplicate()));
    }

    public CustomPacket pollPacketFromQueue() {
        return mPacketDeque.pollFirst();
    }

    public void processIncomingArpPacket(ByteBuffer packet) throws Exception {
        Arp.ArpPacket arpPacket = Arp.parsePacket(packet);

        if (arpPacket instanceof Arp.ArpRequest) {
            Arp.ArpRequest arpRequest = (Arp.ArpRequest)arpPacket;

            mPacketDeque.add(CustomPacket.fromL2Packet(
                    Arp.createResponse(getLocalMacAddress(), arpRequest)
            ));

            addIpToMacPair(arpRequest.getRequesterIp(), arpRequest.getRequesterMac());
        }

        if (arpPacket instanceof Arp.ArpReply) {
            Arp.ArpReply arpReply = (Arp.ArpReply)arpPacket;

            addIpToMacPair(arpReply.getReplyerIp(), arpReply.getReplyerMac());
        }
    }

    private void addIpToMacPair(int ip, long mac) {
        mIpToMac.put(ip, mac);

        if (mIpToPacketsWaitingForMacResolution.containsKey(ip)) {
            mPacketDeque.addAll(mIpToPacketsWaitingForMacResolution.get(ip));
            mIpToPacketsWaitingForMacResolution.remove(ip);
        }
    }

    private int getDestinationIpFromL3Packet(ByteBuffer packet) {
        return packet.getInt(16);
    }

    private int getSourceIpFromL3Packet(ByteBuffer packet) {
        return packet.getInt(12);
    }

    static class CustomPacket {
        private ByteBuffer mPacket;
        private boolean mIsL3;

        private CustomPacket(ByteBuffer packet, boolean isL3) {
            mPacket = packet;
            mIsL3 = isL3;
        }

        public static CustomPacket fromL3Packet(ByteBuffer packet) {
            return new CustomPacket(packet, true);
        }

        public static CustomPacket fromL2Packet(ByteBuffer packet) {
            return new CustomPacket(packet, false);
        }

        public ByteBuffer getPacket() {
            return mPacket;
        }

        public boolean isL3() {
            return mIsL3;
        }
    }

}
