package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l2;

import java.util.Random;

/**
 * Created by kostya on 16/12/2016.
 */

class LocalMacAddressGenerator {

    private static Random random = new Random();

    public static long generateRandomLocallyAdministeredMacAddress() {
        long mac = (random.nextLong() << (2 * 8)) >> (2 * 8);

        // second least significant bit of the first octet must be equal to 1
        mac = mac | (1L << (5 * 8 + 2));
        return mac;
    }
}
