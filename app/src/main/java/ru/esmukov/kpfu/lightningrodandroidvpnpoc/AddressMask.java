package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by kostya on 09/11/2016.
 */

public class AddressMask {
    private String mAddress;  // todo maybe switch to InetAddress
    private int mPrefixLength;

    public AddressMask(String address, int prefixLength) {
        this.mAddress = address;
        this.mPrefixLength = prefixLength;
    }

    public String getAddress() {
        return mAddress;
    }

    public int getPrefixLength() {
        return mPrefixLength;
    }

    /**
     * Creates an AddressMask for a network IP from a host IP.
     * Example: 10.123.123.2/30 -> 10.123.123.0/30
     * @param address 10.123.123.2
     * @param prefixLength 30
     * @return AddressMask
     */
    public static AddressMask networkAddressMask(String address, int prefixLength)
            throws UnknownHostException {
        int ip = pack(InetAddress.getByName(address).getAddress());

        int shiftLength = 32 - prefixLength;
        ip = (ip >> shiftLength) << shiftLength;

        String networkIp = InetAddress.getByAddress(unpack(ip)).getHostAddress();
        return new AddressMask(networkIp, prefixLength);
    }

    private static int pack(byte[] bytes) {
        int val = 0;
        for (int i = 0; i < bytes.length; i++) {
            val <<= 8;
            val |= bytes[i] & 0xff;
        }
        return val;
    }

    private static byte[] unpack(int bytes) {
        return new byte[] {
                (byte)((bytes >>> 24) & 0xff),
                (byte)((bytes >>> 16) & 0xff),
                (byte)((bytes >>>  8) & 0xff),
                (byte)((bytes       ) & 0xff)
        };
    }
}
