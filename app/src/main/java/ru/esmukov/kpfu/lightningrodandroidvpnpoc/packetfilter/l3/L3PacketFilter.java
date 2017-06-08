package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l3;

import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.L3Header;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3.L3HeaderFacade;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.PacketFilter;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l2.PacketInfo;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l2.EthernetHeader;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.serverconnection.ServerConnection;

/**
 * Created by kostya on 16/12/2016.
 */

public class L3PacketFilter implements PacketFilter {
    private static final String TAG = "L3PacketFilter";

    private boolean mPacketInfo;

    private final ByteBuffer ip4HeaderBuffer = ByteBuffer.allocate(L3HeaderFacade.ip4Header.getMinimumHeaderLength());
    private final ByteBuffer packetInfoBuffer = ByteBuffer.allocate(PacketInfo.PI_LEN);
    private final ByteBuffer l3HeaderBuffer = ByteBuffer.allocate(L3HeaderFacade.MAX_L3_HEADER_LEN);
    // max IP packet length is 0xff^2 (len field size in header)
    private final ByteBuffer inBuffer = ByteBuffer.allocate(0xff * 0xff + PacketInfo.PI_LEN);
    private final ByteBuffer outBuffer = ByteBuffer.allocate(0xff * 0xff + PacketInfo.PI_LEN);
    {
        inBuffer.limit(0);
        outBuffer.limit(0);
    }

    public L3PacketFilter(boolean packetInfo) {
        this.mPacketInfo = packetInfo;
    }

    @Override
    public boolean consumeRemote(ServerConnection tunnel) throws IOException {
        // this method consumes single IP packet

        int etherType;
        L3Header l3Header;
        if (mPacketInfo) {
            if (!readPi(tunnel))
                return false;
            etherType = PacketInfo.getAtPos(packetInfoBuffer, 0).getProto();
            l3Header = getL3HeaderByEtherType(etherType);
            while (!readL3Header(tunnel, l3Header)); // we have read PI - we must wait for the header then
        } else {
            // We make IPv4 tunnels only. When PI is absent, only IPv4 packets
            // might be sent by the remote here.
            etherType = L3HeaderFacade.TYPE_IP;
            l3Header = getL3HeaderByEtherType(etherType);
            if (!readL3Header(tunnel, l3Header))
                return false;
        }

        int totalLen = l3Header.getTotalLength(l3HeaderBuffer);
        assert inBuffer.position() == inBuffer.limit() && inBuffer.position() == 0;

        inBuffer.limit(inBuffer.limit() + totalLen);
        inBuffer.put(l3HeaderBuffer);
        while (inBuffer.position() < inBuffer.limit()) {
            tunnel.read(inBuffer);
        }
        if (etherType != L3HeaderFacade.TYPE_IP) {
            // eat up that junk packet, which VpnService will not understand
            Log.w(TAG, "received non-IP etherType: " + etherType);
            inBuffer.limit(0);
        }
        // BufLogger.logIncoming(inBuffer);
        return true;
    }

    @Override
    public boolean produceLocal(FileOutputStream out) throws IOException {
        int toWrite = inBuffer.limit();
        inBuffer.position(0);
        if (toWrite <= 0)
            return false;

        // caution: this *must* be a single IPv4 packet
        out.write(inBuffer.array(), 0, toWrite);
        inBuffer.limit(0);
        return true;
    }

    @Override
    public boolean consumeLocal(FileInputStream in) throws IOException {
        // this method consumes single IP packet

        // We only assign IPv4 addresses on VpnService, thus we assume that
        // only IPv4 packets might be produced here.

        // The whole IP packet must be read from InputStream at once, otherwise the tail will be lost
        assert outBuffer.position() == outBuffer.limit() && outBuffer.position() == 0;
        int read = in.read(outBuffer.array(), 0, outBuffer.capacity());
        if (read <= 0)
            return false;
        outBuffer.limit(read);
        outBuffer.position(0);

        int totalLen = L3HeaderFacade.ip4Header.getTotalLength(outBuffer);
        assert totalLen == outBuffer.limit();

        // BufLogger.logOutgoing(outBuffer);
        return true;
    }

    @Override
    public boolean produceRemote(ServerConnection tunnel) throws IOException {
        int toWrite = outBuffer.limit();
        if (toWrite <= 0)
            return false;

        if (mPacketInfo) {
            writePi(tunnel);
        }
        write(tunnel, outBuffer);
//        BufLogger.logOutgoing(outBuffer);
        outBuffer.limit(0);
//        try { /////////
//            Thread.sleep(30);
//        } catch (InterruptedException e) {
//
//        }
        return true;
    }

    private boolean readL3Header(ServerConnection tunnel, L3Header l3Header) throws IOException {
        l3HeaderBuffer.position(0);
        l3HeaderBuffer.limit(l3Header.getMinimumHeaderLength());
        return readHeader(tunnel, l3HeaderBuffer);
    }

    private void writePi(ServerConnection tunnel) throws IOException {
        packetInfoBuffer.position(0);
        packetInfoBuffer.limit(packetInfoBuffer.capacity());
        PacketInfo.writeAtPos(packetInfoBuffer, 0, EthernetHeader.TYPE_IP);
        write(tunnel, packetInfoBuffer);
    }

    private void write(ServerConnection tunnel, ByteBuffer buffer) throws IOException {
        int toWrite = outBuffer.limit();
        outBuffer.position(0);
        while (toWrite > 0) {
            toWrite -= tunnel.write(outBuffer);
        }
    }

    private boolean readPi(ServerConnection tunnel) throws IOException {
        packetInfoBuffer.position(0);
        packetInfoBuffer.limit(packetInfoBuffer.capacity());
        return readHeader(tunnel, packetInfoBuffer);
    }

    private boolean readHeader(ServerConnection tunnel, ByteBuffer target) throws IOException {
        do {
            tunnel.read(target);
        } while (target.position() > 0 && target.position() < target.limit());
        if (target.position() <= 0) {
            return false;
        }
        target.position(0);
        return true;
    }

    private L3Header getL3HeaderByEtherType(int etherType) {
        L3Header l3Header = L3HeaderFacade.fromEthertype(etherType);
        if (l3Header == null) {
            // We must fail here, because we can't determine packet length
            // if we don't know exact etherType.
            throw new RuntimeException("Unknown etherType: " + etherType);
        }
        return l3Header;
    }


//
//    @Override
//    public boolean fromLocalToRemote(ByteBuffer ip4Packet) {
//        if (mPacketInfo) {
//            PacketInfo.prepend(ip4Packet, EthernetHeader.TYPE_IP);
//        }
//        return true;
//    }
//
//    @Override
//    public boolean fromRemoteToLocal(ByteBuffer remotePacket) {
//        remoteBuffer.put(remotePacket);
//
//        if (mPacketInfo) {
//            try {
//                int proto = PacketInfo.strip(remotePacket).getProto();
//
//                if (proto != EthernetHeader.TYPE_IP) {
//                    // skin non-IP packets
//                    Log.w(TAG, "received non-IP proto: " + proto);
//                    return false;
//                }
//            } catch (Exception e) {
//                Log.i(TAG, "Packet info exception", e);
//                return false;
//            }
//        }
//        return true;
//    }
//
//    @Override
//    public boolean nextCustomPacketToRemote(ByteBuffer remotePacket) {
//        return false;
//    }
}
