package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l2.EthernetHeader;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l2.PacketInfo;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.L3Header;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.L3HeaderFacade;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.UnknownL3PacketException;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection.ServerConnection;

/**
 * Created by kostya on 13/06/2017.
 */

public abstract class BasePacketFilter implements PacketFilter {
    protected boolean mPacketInfo;
    protected boolean mSlip;

    protected final ByteBuffer packetInfoBuffer = ByteBuffer.allocate(PacketInfo.PI_LEN);
    protected final ByteBuffer inPacketHeaderBuffer = ByteBuffer.allocate(Math.max(L3HeaderFacade.MAX_L3_HEADER_LEN, EthernetHeader.ETHERNET_HEADER_LENGTH));
    // max IP packet length is 0xff^2 (len field size in header)
    protected final ByteBuffer inIp4Buffer = ByteBuffer.allocate(0xff * 0xff + PacketInfo.PI_LEN + EthernetHeader.ETHERNET_HEADER_LENGTH);
    protected final ByteBuffer outTunBuffer = ByteBuffer.allocate(0xff * 0xff + PacketInfo.PI_LEN + EthernetHeader.ETHERNET_HEADER_LENGTH);
    {
        inIp4Buffer.limit(0);
        outTunBuffer.limit(0);
    }

    public BasePacketFilter(boolean packetInfo, boolean slip) {
        mPacketInfo = packetInfo;
        mSlip = slip;
    }

    @Override
    public boolean produceLocal(FileOutputStream out) throws IOException {
        int toWrite = inIp4Buffer.limit();
        inIp4Buffer.position(0);
        if (toWrite <= 0)
            return false;

        // caution: this *must* be a single IPv4 packet
        out.write(inIp4Buffer.array(), 0, toWrite);
        inIp4Buffer.limit(0);
        return true;
    }

    @Override
    public boolean consumeLocal(FileInputStream in) throws IOException {
        // this method consumes single IP packet

        // We only assign IPv4 addresses on VpnService, thus we assume that
        // only IPv4 packets might be produced here.

        // The whole IP packet must be read from InputStream at once, otherwise the tail will be lost
        assert outTunBuffer.position() == outTunBuffer.limit() && outTunBuffer.position() == 0;
        int read = in.read(outTunBuffer.array(), 0, outTunBuffer.capacity());
        if (read <= 0)
            return false;
        outTunBuffer.limit(read);
        outTunBuffer.position(0);

        int totalLen = L3HeaderFacade.ip4Header.getTotalLength(outTunBuffer);
        assert totalLen == outTunBuffer.limit();

        // BufLogger.logOutgoing(outTunBuffer);
        return true;
    }

    protected void writeFrame(ServerConnection tunnel, ByteBuffer buffer) throws IOException {
        if (mSlip) {
            Slip.slipEncodeFrame(buffer);
        }
        int toWrite = buffer.limit();
        buffer.position(0);
        while (toWrite > 0) {
            toWrite -= tunnel.write(buffer);
        }
    }

    protected void read(ServerConnection tunnel, ByteBuffer buffer) throws IOException {
        int prevPos = buffer.position();
        boolean bufIncreased = false;
        while (true) {
            if (mSlip && buffer.position() == buffer.limit()) {
                // if buffer is full, then we have already read something. but we're still here,
                // that means that buffer ends with Slip.ESC. So increase buffer for 1 byte,
                // to read what follows the Slip.ESC and perform SLIP transformation below.
                bufIncreased = true;
                buffer.limit(buffer.limit() + 1);
            }
            tunnel.read(buffer);
            if (mSlip) {
                try {
                    if (!Slip.slipStrip(buffer, prevPos)) {
                        // It means that we have Slip.ESC at the end. In this case we have
                        // to read at least 1 more byte, to perform a SLIP transformation.
                        continue;
                    }
                } finally {
                    if (bufIncreased) {
                        buffer.limit(buffer.limit() - 1);
                    }
                }
            }
            break;
        }
    }

    protected boolean readL3Header(ServerConnection tunnel, L3Header l3Header) throws IOException {
        inPacketHeaderBuffer.position(0);
        inPacketHeaderBuffer.limit(l3Header.getMinimumHeaderLength());
        return readHeader(tunnel, inPacketHeaderBuffer);
    }

    protected boolean readHeader(ServerConnection tunnel, ByteBuffer target) throws IOException {
        do {
            read(tunnel, target);
        } while (target.position() > 0 && target.position() < target.limit());
        if (target.position() <= 0) {
            return false;
        }
        target.position(0);
        return true;
    }

    protected L3Header getL3HeaderByEtherType(int etherType) throws UnknownL3PacketException {
        L3Header l3Header = L3HeaderFacade.fromEthertype(etherType);
        if (l3Header == null) {
            // We must fail here, because we can't determine packet length
            // if we don't know exact etherType.
            throw new UnknownL3PacketException("Unknown etherType: " + etherType);
        }
        return l3Header;
    }
}
