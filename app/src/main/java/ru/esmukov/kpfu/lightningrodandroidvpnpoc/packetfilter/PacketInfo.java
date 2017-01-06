package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter;

import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 06/01/2017.
 */

public class PacketInfo {
    private static final String TAG = "PacketInfo";

    public static void prepend(ByteBuffer packet, int etherType) {
        // todo looks like we don't need to ever set the TUN_PKT_STRIP flag, right?
        // see:
        // static ssize_t tun_put_user(struct tun_struct *tun,
        //                             struct tun_file *tfile,
        //                             struct sk_buff *skb,
        //                             struct iov_iter *iter)

        // http://lxr.free-electrons.com/source/include/uapi/linux/if_ether.h
        short flags = 0;

        ByteBufferUtils.moveRight(packet, 4);

        packet.putShort(0, flags);

        // Ethertype as per http://lxr.free-electrons.com/source/include/uapi/linux/if_ether.h
        packet.putShort(2, (short) etherType);
    }

    public static PacketInfoHeader strip(ByteBuffer remotePacket) throws Exception {
        int length = remotePacket.limit();

        if (length < 4) {
            throw new Exception("packet with invalid length (<4)");
        }

        /*
        struct tun_pi {
            __u16  flags;
            __be16 proto;
        };
         */

        int flags = ((remotePacket.get(0) & 0xFF) << 8) | (remotePacket.get(1) & 0xFF);
        int proto = ((remotePacket.get(2) & 0xFF) << 8) | (remotePacket.get(3) & 0xFF);

        if (flags != 0) {
            Log.w(TAG, "received non-zero flags: " + flags);
        }

        // todo ?? check PI protocol as per
        // static ssize_t tun_get_user(struct tun_struct *tun, struct tun_file *tfile,
        //                             void *msg_control, struct iov_iter *from,
        //                             int noblock)

        ByteBufferUtils.moveLeft(remotePacket, 4);

        return new PacketInfoHeader(proto);
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
