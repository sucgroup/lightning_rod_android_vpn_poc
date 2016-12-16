package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 16/12/2016.
 */

public class ByteBufferUtils {

    public static void moveLeft(ByteBuffer buffer, int shiftBytes) {
        int length = buffer.limit();

        for (int i = 0; i < length; i++) {
            buffer.put(i, buffer.get(i + shiftBytes));
        }
        buffer.limit(length - shiftBytes);
    }

    public static void moveRight(ByteBuffer buffer, int shiftBytes) {
        int length = buffer.limit();

        buffer.limit(length + shiftBytes);
        for (int i = length - 1; i >= 0; i--) {
            buffer.put(i + shiftBytes, buffer.get(i));
        }
    }

    public static void putFromOneIntoAnother(ByteBuffer from, ByteBuffer to) {
        to.limit(from.limit());
        to.put(from.array(), 0, from.limit());
    }
}
