package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l3;

import android.util.Log;

import java.io.IOException;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l2.EthernetHeader;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.Ip4Header;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.L3Header;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.L3HeaderFacade;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.UnknownL3PacketException;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.BasePacketFilter;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.ByteBufferUtils;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.PacketFilter;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l2.PacketInfo;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection.ServerConnection;

/**
 * Created by kostya on 16/12/2016.
 */

public class L3PacketFilter extends BasePacketFilter implements PacketFilter {
    private static final String TAG = "L3PacketFilter";

    public L3PacketFilter(boolean packetInfo, boolean slip) {
        super(packetInfo, slip);
    }

    @Override
    public boolean consumeRemote(ServerConnection tunnel) throws IOException {
        // this method consumes single IP packet

        L3Header l3Header;
        if (mPacketInfo) {
            l3Header = readL3HeaderWithPi(tunnel);
            if (l3Header == null)
                return false;
        } else {
            // We make IPv4 tunnels only. When PI is absent, only IPv4 packets
            // might be sent by the remote here.
            int etherType = L3HeaderFacade.TYPE_IP;
            l3Header = getL3HeaderByEtherType(etherType);
            if (!readL3Header(tunnel, l3Header))
                return false;
        }

        int totalLen = l3Header.getTotalLength(inPacketHeaderBuffer);
        assert inIp4Buffer.position() == inIp4Buffer.limit() && inIp4Buffer.position() == 0;

        inIp4Buffer.limit(totalLen);
        inIp4Buffer.put(inPacketHeaderBuffer);
        while (inIp4Buffer.position() < inIp4Buffer.limit()) {
            read(tunnel, inIp4Buffer);
        }
        if (!(l3Header instanceof Ip4Header)) {
            // eat up that junk packet, which VpnService will not understand
            Log.w(TAG, "received non-IP etherType: " + l3Header);
            inIp4Buffer.limit(0);
        }
        // BufLogger.logIncoming(inIp4Buffer);
        return true;
    }

    @Override
    public boolean produceRemote(ServerConnection tunnel) throws IOException {
        int toWrite = outTunBuffer.limit();
        if (toWrite <= 0)
            return false;

        if (mPacketInfo) {
            putPiToOutbuffer();
        }
        outTunBuffer.position(0);
        writeFrame(tunnel, outTunBuffer);
        outTunBuffer.limit(0);
        return true;
    }

    private void putPiToOutbuffer() throws IOException {
        ByteBufferUtils.moveRight(outTunBuffer, PacketInfo.PI_LEN);
        PacketInfo.writeAtPos(outTunBuffer, 0, EthernetHeader.TYPE_IP);
    }

    private L3Header readL3HeaderWithPi(ServerConnection tunnel) throws IOException, UnknownL3PacketException {
        L3Header l3Header = null;
        while (true) {
            packetInfoBuffer.position(0);
            packetInfoBuffer.limit(packetInfoBuffer.capacity());
            if (l3Header == null) {
                // don't block if nothing has read yet
                if (!readHeader(tunnel, packetInfoBuffer))
                    return null;
            } else {
                // wait for the rest of the packet we've already started to read
                while (!readHeader(tunnel, packetInfoBuffer));
            }

            if (l3Header != null) {
                // we need to determine what have we just read: another PI or a real packet
                packetInfoBuffer.position(0);
                if (l3Header.isMatch(packetInfoBuffer))
                    break; // it's the real packet here
                // it was an another PI - go on
            }
            int etherType = PacketInfo.getAtPos(packetInfoBuffer, 0).getProto();
            l3Header = getL3HeaderByEtherType(etherType);
        }

        packetInfoBuffer.position(0);
        inPacketHeaderBuffer.position(0);
        inPacketHeaderBuffer.limit(l3Header.getMinimumHeaderLength());
        inPacketHeaderBuffer.put(packetInfoBuffer);

        while (!readHeader(tunnel, inPacketHeaderBuffer));
        return l3Header;
    }

}
