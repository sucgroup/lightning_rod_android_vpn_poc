package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l2;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 16/12/2016.
 */

class Arp {

    private static final short OPCODE_REQUEST = 0x1;
    private static final short OPCODE_REPLY = 0x2;

    public static ByteBuffer request(long senderMac, int senderIp, int targetIp) {
        ByteBuffer packet = ByteBuffer.allocate(32767);
        packet.position(0);

        packet.putShort((short)1); // hardware type -- ethernet
        packet.putShort((short)EthernetHeader.TYPE_IP); // protocol type -- IP
        packet.put((byte)6); // hardware size
        packet.put((byte)4); // protocol size
        packet.putShort(OPCODE_REQUEST); // opcode -- request

        // todo:
        // sender mac
        // sender ip
        // target mac
        // target ip

        return packet;
    }

    public static ArpReply response(ByteBuffer packet) throws Exception {
        packet.position(0);
        short hardwareType = packet.getShort();
        short protocolType = packet.getShort();
        byte hardwareSize = packet.get();
        byte protocolSize = packet.get();
        short opcode = packet.getShort();

        if (hardwareType != 1)
            throw new Exception("Bad hardware type: " + hardwareType);

        if (protocolType != EthernetHeader.TYPE_IP)
            throw new Exception("Bad protocol type: " + protocolType);

        if (hardwareSize != 6)
            throw new Exception("Bad hardware size: " + hardwareSize);

        if (protocolSize != 4)
            throw new Exception("Bad protocol size: " + protocolSize);

        if (opcode != OPCODE_REPLY)
            throw new Exception("Bad opcode: " + opcode);

        // todo:
        long senderMac;
        int senderIp;
        long targetMac;
        int targetIp;

        return new ArpReply(senderMac, senderIp, targetMac, targetIp);
    }


    public static class ArpReply {

        private long mSenderMac;
        private int mSenderIp;
        private long mTargetMac;
        private int mTargetIp;

        private ArpReply(long senderMac, int senderIp, long targetMac, int targetIp) {
            mSenderMac = senderMac;
            mSenderIp = senderIp;
            mTargetMac = targetMac;
            mTargetIp = targetIp;
        }

        public long getReplyerMac() {
            return mSenderMac;
        }

        public int getReplyerIp() {
            return mSenderIp;
        }
    }

}
