package ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3;

import java.nio.ByteBuffer;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.ByteBufferUtils;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l2.EthernetHeader;

/**
 * Created by kostya on 16/12/2016.
 */

public class ArpHeader implements L3Header {
    // https://en.wikipedia.org/wiki/Address_Resolution_Protocol#Packet_structure

    private static final short HARDWARE_TYPE_ETHERNET = 1;
    private static final short OPCODE_REQUEST = 0x1;
    private static final short OPCODE_REPLY = 0x2;

    /**
     * ARP Request frame (L2 packet with Ethernet headers)
     *
     * @param localMac
     * @param localIp
     * @param targetIp
     * @return
     */
    public static ByteBuffer createRequestFrame(long localMac, int localIp, int targetIp) {
        return createFrame(OPCODE_REQUEST,
                localMac, localIp,
                EthernetHeader.BROADCAST_MAC, targetIp);
    }

    /**
     * ARP Response frame (L2 packet with Ethernet headers)
     *
     * @param localMac
     * @param arpRequest
     * @return
     */
    public static ByteBuffer createResponseFrame(long localMac, ArpRequest arpRequest) {
        return createFrame(OPCODE_REPLY,
                localMac, arpRequest.getRequestedIp(),
                arpRequest.getRequesterMac(), arpRequest.getRequesterIp());
    }

    /**
     * Parse L3 packet (without Ethernet headers)
     *
     * @param packet
     * @return
     * @throws Exception
     */
    public static ArpPacket parsePacket(ByteBuffer packet) throws Exception {
        packet.position(0);

        short hardwareType = packet.getShort();
        short protocolType = packet.getShort();
        byte hardwareSize = packet.get();
        byte protocolSize = packet.get();
        short opcode = packet.getShort();

        if (hardwareType != HARDWARE_TYPE_ETHERNET)
            throw new Exception("Bad hardware type: " + hardwareType);

        if (protocolType != EthernetHeader.TYPE_IP)
            throw new Exception("Bad protocol type: " + protocolType);

        if (hardwareSize != 6)
            throw new Exception("Bad hardware size: " + hardwareSize);

        if (protocolSize != 4)
            throw new Exception("Bad protocol size: " + protocolSize);

        long senderMac = ByteBufferUtils.get6bytes(packet);
        int senderIp = packet.getInt();
        long targetMac = ByteBufferUtils.get6bytes(packet);
        int targetIp = packet.getInt();

        return ArpPacket.create(opcode, senderMac, senderIp, targetMac, targetIp);
    }

    /**
     * Create L3 ARP packet
     *
     * @param opcode
     * @param senderMac
     * @param senderIp
     * @param targetMac
     * @param targetIp
     * @return
     */
    private static ByteBuffer createPacket(short opcode,
                                           long senderMac, int senderIp,
                                           long targetMac, int targetIp) {
        ByteBuffer packet = ByteBuffer.allocate(256);
        packet.position(0);

        packet.putShort(HARDWARE_TYPE_ETHERNET); // hardware type -- ethernet
        packet.putShort((short) EthernetHeader.TYPE_IP); // protocol type -- IP
        packet.put((byte) 6); // hardware size
        packet.put((byte) 4); // protocol size
        packet.putShort(opcode); // opcode -- createRequestFrame

        ByteBufferUtils.put6bytes(packet, senderMac);
        packet.putInt(senderIp);
        ByteBufferUtils.put6bytes(packet, targetMac);
        packet.putInt(targetIp);

        packet.limit(packet.position());
        packet.position(0);

        return packet;
    }

    /**
     * Create ARP frame (L2 Ethernet headers + L3 ARP packet)
     *
     * @param opcode
     * @param senderMac
     * @param senderIp
     * @param targetMac
     * @param targetIp
     * @return
     */
    private static ByteBuffer createFrame(short opcode,
                                          long senderMac, int senderIp,
                                          long targetMac, int targetIp) {
        ByteBuffer packet = createPacket(opcode, senderMac, senderIp, targetMac, targetIp);

        new EthernetHeader(targetMac, senderMac, EthernetHeader.TYPE_ARP).addToPacket(packet);

        return packet;
    }

    public static class ArpPacket {
        protected long mSenderMac;
        protected int mSenderIp;
        protected long mTargetMac;
        protected int mTargetIp;

        private ArpPacket(long senderMac, int senderIp,
                          long targetMac, int targetIp) throws Exception {
            mSenderMac = senderMac;
            mSenderIp = senderIp;
            mTargetMac = targetMac;
            mTargetIp = targetIp;
        }

        private static ArpPacket create(short opcode,
                                        long senderMac, int senderIp,
                                        long targetMac, int targetIp) throws Exception {

            if (opcode == OPCODE_REPLY)
                return new ArpReply(senderMac, senderIp, targetMac, targetIp);

            if (opcode == OPCODE_REQUEST)
                return new ArpRequest(senderMac, senderIp, targetMac, targetIp);

            throw new Exception("Bad opcode: " + opcode);

        }
    }

    public static class ArpReply extends ArpPacket {
        private ArpReply(long senderMac, int senderIp,
                         long targetMac, int targetIp) throws Exception {
            super(senderMac, senderIp, targetMac, targetIp);
        }

        public long getReplyerMac() {
            return mSenderMac;
        }

        public int getReplyerIp() {
            return mSenderIp;
        }
    }

    public static class ArpRequest extends ArpPacket {
        private ArpRequest(long senderMac, int senderIp,
                           long targetMac, int targetIp) throws Exception {
            super(senderMac, senderIp, targetMac, targetIp);
        }

        public long getRequesterMac() {
            return mSenderMac;
        }

        public int getRequesterIp() {
            return mSenderIp;
        }

        public int getRequestedIp() {
            return mTargetIp;
        }
    }

    @Override
    public int getMinimumHeaderLength() {
        return 28;
    }

    @Override
    public int getTotalLength(ByteBuffer packet) {
        return 28;
    }

    @Override
    public boolean isMatch(ByteBuffer packet) {
        return packet.limit() >= 2 && packet.getShort() == HARDWARE_TYPE_ETHERNET;
    }
}
