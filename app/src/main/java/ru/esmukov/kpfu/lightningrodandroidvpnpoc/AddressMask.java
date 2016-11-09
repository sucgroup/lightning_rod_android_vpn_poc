package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

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
}
