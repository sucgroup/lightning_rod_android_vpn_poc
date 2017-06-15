package ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3;

import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by kostya on 06/06/2017.
 */

public class L3HeaderFacade {
    // http://www.iana.org/assignments/ieee-802-numbers/ieee-802-numbers.xhtml#ieee-802-numbers-1
    public static final int TYPE_ARP = 0x0806;
    public static final int TYPE_IP = 0x0800;
    public static final int TYPE_IPV6 = 0x86DD;

    // Cache instances for performance considerations
    public static final Ip4Header ip4Header = new Ip4Header();
    public static final Ip6Header ip6Header = new Ip6Header();
    public static final ArpHeader arpHeader = new ArpHeader();
    public static final int MAX_L3_HEADER_LEN = maxL3HeaderLength();

    private static int maxL3HeaderLength() {
        return Collections.max(Arrays.asList(
                ip4Header.getMinimumHeaderLength(),
                ip6Header.getMinimumHeaderLength(),
                arpHeader.getMinimumHeaderLength()
        ));
    }

    public static @Nullable L3Header fromEthertype(int etherType) {
        switch (etherType) {
            case TYPE_IP:
                return ip4Header;
            case TYPE_IPV6:
                return ip6Header;
            case TYPE_ARP:
                return arpHeader;
            default:
                return null;
        }
    }


}
