package ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l2;

import android.util.Log;

import java.nio.ByteBuffer;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.ByteBufferUtils;

/**
 * Created by kostya on 06/01/2017.
 */

public class PacketInfo {
    private static final String TAG = "PacketInfo";

    public static int PI_LEN = 4;

    public static void writeAtPos(ByteBuffer packet, int pos, int etherType) {
        // todo looks like we don't need to ever set the TUN_PKT_STRIP flag, right?
        // see:
        // static ssize_t tun_put_user(struct tun_struct *tun,
        //                             struct tun_file *tfile,
        //                             struct sk_buff *skb,
        //                             struct iov_iter *iter)

        // http://lxr.free-electrons.com/source/include/uapi/linux/if_ether.h
        short flags = 0;

        packet.putShort(pos, flags);

        // Ethertype as per http://lxr.free-electrons.com/source/include/uapi/linux/if_ether.h
        packet.putShort(pos + 2, (short) etherType);
    }

    @Deprecated
    public static void prepend(ByteBuffer packet, int etherType) {
        ByteBufferUtils.moveRight(packet, PI_LEN);
        writeAtPos(packet, 0, etherType);
    }

    public static PacketInfoHeader getAtPos(ByteBuffer remotePacket, int pos) {
        int length = remotePacket.limit() - pos;

        if (length < 4) {
            throw new RuntimeException("packet with invalid length (<4)");
        }

        /*
        struct tun_pi {
            __u16  flags;
            __be16 proto;
        };
         */

        int flags = ((remotePacket.get(pos) & 0xFF) << 8) | (remotePacket.get(pos + 1) & 0xFF);
        int proto = ((remotePacket.get(pos + 2) & 0xFF) << 8) | (remotePacket.get(pos + 3) & 0xFF);

        if (flags != 0) {
            Log.w(TAG, "received non-zero flags: " + flags);
        }

        // todo ?? check PI protocol as per
        // static ssize_t tun_get_user(struct tun_struct *tun, struct tun_file *tfile,
        //                             void *msg_control, struct iov_iter *from,
        //                             int noblock)

        return new PacketInfoHeader(proto);
    }

    @Deprecated
    public static PacketInfoHeader strip(ByteBuffer remotePacket) {
        PacketInfoHeader pi = getAtPos(remotePacket, 0);
        ByteBufferUtils.moveLeft(remotePacket, PI_LEN);
        return pi;
    }

    public static class PacketInfoHeader {
        private int mProto;

        private PacketInfoHeader(int mProto) {
            this.mProto = mProto;
        }

        public int getProto() {
            return mProto;
        }
    }

}
