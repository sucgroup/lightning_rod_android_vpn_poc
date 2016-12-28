package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 16/12/2016.
 */

public class ByteBufferUtils {

    public static void moveLeft(ByteBuffer buffer, int shiftBytes) {
        int length = buffer.limit();

        for (int i = shiftBytes; i < length; i++) {
            buffer.put(i - shiftBytes, buffer.get(i));
        }
        buffer.position(length - shiftBytes);
        buffer.limit(length - shiftBytes);
    }

    public static void moveRight(ByteBuffer buffer, int shiftBytes) {
        int length = buffer.limit();

        buffer.limit(length + shiftBytes);
        for (int i = length - 1; i >= 0; i--) {
            buffer.put(i + shiftBytes, buffer.get(i));
        }
        buffer.position(length + shiftBytes);
    }

    public static void putFromOneIntoAnother(ByteBuffer from, ByteBuffer to) {
        to.limit(from.limit() + 1); // java.nio.BufferOverflowException
        to.put(from.array(), 0, from.limit());
        to.limit(from.limit());
    }

    public static void put6bytes(ByteBuffer byteBuffer, long l) {
        // first byte
        byteBuffer.put((byte)((l >>> (8 * 5)) & 0xFF));
        // second byte
        byteBuffer.put((byte)((l >>> (8 * 4)) & 0xFF));
        // rest 4 bytes
        byteBuffer.putInt((int)(l & 0xFFFFFFFFL));
    }

    public static long get6bytes(ByteBuffer byteBuffer) {
        long l = 0;
        l |= (long)(byteBuffer.get() & 0xFF) << (8 * 5);
        l |= (long)(byteBuffer.get() & 0xFF) << (8 * 4);
        l |= byteBuffer.getInt() & 0xFFFFFFFFL;
        return l;
    }
}
