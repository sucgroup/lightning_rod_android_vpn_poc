package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l3;

import android.util.Log;

import java.nio.ByteBuffer;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.ByteBufferUtils;

/**
 * Created by kostya on 16/12/2016.
 */

/**
 * Implements Linux tun/tap PACKET INFO concept (IFF_NO_PI).
 *
 * @see <a href="https://www.kernel.org/doc/Documentation/networking/tuntap.txt">tuntap.txt</a>
 * @see <a href="http://lxr.free-electrons.com/source/include/uapi/linux/if_ether.h">if_ether.h</a>
 */
class L3PacketInfo {
    private static final String TAG = "PacketInfoPacketFilter";

    // http://lxr.free-electrons.com/source/include/uapi/linux/if_ether.h
    private static final int ETH_P_IP = 0x0800;
    // private static final int ETH_P_IPV6 = 0x86DD;

    static boolean fromLocalToRemote(ByteBuffer packet) {
        int length = packet.limit();

        // todo looks like we don't need to ever set the TUN_PKT_STRIP flag, right?
        // see:
        // static ssize_t tun_put_user(struct tun_struct *tun,
        //                             struct tun_file *tfile,
        //                             struct sk_buff *skb,
        //                             struct iov_iter *iter)

        // http://lxr.free-electrons.com/source/include/uapi/linux/if_ether.h
        short flags = 0;
        short proto = ETH_P_IP;

        ByteBufferUtils.moveRight(packet, 4);

        packet.putShort(0, flags);
        packet.putShort(2, proto);

        // packet.limit(length + 4);

        return true;
    }

    static boolean fromRemoteToLocal(ByteBuffer packet) {
        int length = packet.limit();

        if (length < 4) {
            Log.i(TAG, "dropped packet with invalid length (<4)");
            return false;
        }

        /*
        struct tun_pi {
            __u16  flags;
            __be16 proto;
        };
         */

        int flags = ((packet.get(0) & 0xFF) << 8) | (packet.get(1) & 0xFF);
        int proto = ((packet.get(2) & 0xFF) << 8) | (packet.get(3) & 0xFF);

        if (flags != 0) {
            Log.w(TAG, "received non-zero flags: " + flags);
        }

        if (proto != ETH_P_IP) {
            // skin non-IP packets
            Log.w(TAG, "received non-IP proto: " + proto);
            return false;
        }

        // todo check PI protocol as per
        // static ssize_t tun_get_user(struct tun_struct *tun, struct tun_file *tfile,
        //                             void *msg_control, struct iov_iter *from,
        //                             int noblock)

        ByteBufferUtils.moveLeft(packet, 4);

        return true;
    }

}
