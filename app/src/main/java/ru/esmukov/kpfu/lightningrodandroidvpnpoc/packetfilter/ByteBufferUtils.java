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

    public static void put6bytes(ByteBuffer byteBuffer, long l) {
        // first byte
        byteBuffer.put((byte)(l >> (8 * 5)));
        // second byte
        byteBuffer.put((byte)(l >> (8 * 4)));
        // rest 4 bytes
        byteBuffer.putInt((int)(l));
    }

    public static long get6bytes(ByteBuffer byteBuffer) {
        long l = 0;
        l |= (long)byteBuffer.get() << (8 * 5);
        l |= (long)byteBuffer.get() << (8 * 4);
        l |= (long)byteBuffer.getInt();
        return l;
    }
}
