package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter;

import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 06/06/2017.
 */

public class BufLogger {
    private static final String TAG = "BufLogger";

    public static void logOutgoing(ByteBuffer packet) {
        Log.i(TAG + " outgoing", bytesToHex(packet.array(), packet.limit()));
    }

    public static void logIncoming(ByteBuffer packet) {
        Log.i(TAG + " incoming", bytesToHex(packet.array(), packet.limit()));
    }

    // https://stackoverflow.com/a/9855338
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes, int limit) {
        char[] hexChars = new char[limit * 3];
        for ( int j = 0; j < limit; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }

}
