package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l2;

import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.SocatServerConnectionInfo;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l2.EthernetHeader;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.ArpHeader;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.Ip4Header;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.Ip6Header;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.L3Header;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.BasePacketFilter;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.ByteBufferUtils;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.PacketFilter;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l2.PacketInfo;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection.ServerConnection;

/**
 * Created by kostya on 16/12/2016.
 */

public class L2ToL3PacketFilter extends BasePacketFilter implements PacketFilter {
    private static final String TAG = "L2ToL3PacketFilter";

    private final MacResolver mMacResolver;

    public L2ToL3PacketFilter(boolean packetInfo,
                              boolean slip,
                              SocatServerConnectionInfo.InterfaceInfo interfaceInfo) {
        super(packetInfo, slip);
        mMacResolver = new MacResolver(interfaceInfo);
    }

    @Override
    public boolean consumeRemote(ServerConnection tunnel) throws IOException {
        EthernetHeader ethernetHeader;
        if (mPacketInfo) {
            ethernetHeader = readL2HeaderWithPi(tunnel);
        } else {
            ethernetHeader = readL2Header(tunnel);
        }
        if (ethernetHeader == null)
            return false;

        L3Header l3Header = getL3HeaderByEtherType(ethernetHeader.getEtherType());
        while (!readL3Header(tunnel, l3Header));
        int totalLen = l3Header.getTotalLength(inPacketHeaderBuffer);

        // inIp4Buffer below contains any L3 packet, not just IPv4.
        // but in the end only IPv4 packet will be left in this buffer.
        assert inIp4Buffer.position() == inIp4Buffer.limit() && inIp4Buffer.position() == 0;

        inIp4Buffer.limit(totalLen);
        inIp4Buffer.put(inPacketHeaderBuffer);
        while (inIp4Buffer.position() < inIp4Buffer.limit()) {
            read(tunnel, inIp4Buffer);
        }

        if (l3Header instanceof Ip4Header) {
            if (!mMacResolver.shouldFrameBeAccepted(ethernetHeader.getDestinationMac()))
                inIp4Buffer.limit(0); // drop foreign IPv4 packet
        } else if (l3Header instanceof Ip6Header) {
            // silently drop any IPV6 communications
            inIp4Buffer.limit(0);
        } else if (l3Header instanceof ArpHeader) {
            try {
                mMacResolver.processIncomingArpPacket(inIp4Buffer);
            } catch (Exception e) {
                Log.i(TAG, "ARP bad incoming packet", e);
            } finally {
                inIp4Buffer.limit(0);
            }
        } else {
            throw new RuntimeException("Unknown l3Header. (have you just added a new L3 header class?)");
        }
        return true;
    }

    @Override
    public boolean consumeLocal(FileInputStream in) throws IOException {
        if (!super.consumeLocal(in))
            return false;

        convertL3ToL2();
        return true;
    }

    @Override
    public boolean produceRemote(ServerConnection tunnel) throws IOException {
        boolean anyProduced = produceL2Packet(tunnel); // send packet in the buf

        MacResolver.CustomPacket customPacket;

        while (true) {
            customPacket = mMacResolver.pollPacketFromQueue();
            if (customPacket == null)
                break;
            ByteBufferUtils.copy(customPacket.getPacket(), outTunBuffer);
            if (customPacket.isL3())
                convertL3ToL2();  // might queue packet and empty the buf
            anyProduced = produceL2Packet(tunnel) || anyProduced;
        }
        return anyProduced;
    }

    private boolean produceL2Packet(ServerConnection tunnel) throws IOException {
        int toWrite = outTunBuffer.limit();
        if (toWrite <= 0)
            return false;

        if (mPacketInfo) {
            putPiToOutbuffer(EthernetHeader.getEtherType(outTunBuffer));
        }
        outTunBuffer.position(0);
//        BufLogger.logOutgoing(outTunBuffer);
        writeFrame(tunnel, outTunBuffer);
        outTunBuffer.limit(0);
        return true;
    }

    private void putPiToOutbuffer(int etherType) throws IOException {
        ByteBufferUtils.moveRight(outTunBuffer, PacketInfo.PI_LEN);
        PacketInfo.writeAtPos(outTunBuffer, 0, etherType);
    }

    private void convertL3ToL2() {
        // outTunBuffer is an IP4 packet
        Long destinationMac = mMacResolver.getDestinationMacAddressByL3IpPacket(outTunBuffer);

        if (destinationMac == null) {
            mMacResolver.resolveMacAndQueueL3IpPacket(outTunBuffer);
            // unknown destination mac as for now - remove the packet
            outTunBuffer.limit(0);
        } else {
            // we know destination mac - keep that packet in the buffer
            EthernetHeader ethernetHeader = new EthernetHeader(destinationMac,
                    mMacResolver.getLocalMacAddress(), EthernetHeader.TYPE_IP);
            ethernetHeader.addToPacket(outTunBuffer);
        }
    }

    private EthernetHeader readL2HeaderWithPi(ServerConnection tunnel) throws IOException {
        int etherType = -1;
        inPacketHeaderBuffer.position(0);
        inPacketHeaderBuffer.limit(EthernetHeader.ETHERNET_HEADER_LENGTH);
        while (true) {
            // fill buf
            if (etherType == -1) {
                // don't block if nothing has read yet
                if (!readHeader(tunnel, inPacketHeaderBuffer))
                    return null;
            } else {
                // wait for the rest of the packet we've already started to read
                while (!readHeader(tunnel, inPacketHeaderBuffer));
            }

            if (etherType != -1) {
                // we need to determine what have we just read: another PI or a real packet
                if (EthernetHeader.isMatch(inPacketHeaderBuffer, etherType))
                    // it's the real packet here
                    return EthernetHeader.fromFrame(inPacketHeaderBuffer);
                // it was an another PI - go on
            }
            etherType = PacketInfo.getAtPos(inPacketHeaderBuffer, 0).getProto();

            // strip off PI
            int oldLen = inPacketHeaderBuffer.limit();
            ByteBufferUtils.moveLeft(inPacketHeaderBuffer, PacketInfo.PI_LEN);
            inPacketHeaderBuffer.position(oldLen - PacketInfo.PI_LEN);
            inPacketHeaderBuffer.limit(oldLen);
        }
    }

    private EthernetHeader readL2Header(ServerConnection tunnel) throws IOException {
        inPacketHeaderBuffer.position(0);
        inPacketHeaderBuffer.limit(EthernetHeader.ETHERNET_HEADER_LENGTH);
        if (!readHeader(tunnel, inPacketHeaderBuffer))
            return null;
        return EthernetHeader.fromFrame(inPacketHeaderBuffer);
    }

}
