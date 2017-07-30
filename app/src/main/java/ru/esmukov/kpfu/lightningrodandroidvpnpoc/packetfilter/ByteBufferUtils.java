package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 16/12/2016.
 */

public class ByteBufferUtils {

    // ByteBuffer.get/ByteBuffer.put are VERY slow. copying to a byte array,
    // modifying it and putting it back is a lot faster.
    private final static byte[] mLocalBuf = new byte[2<<17];

    public static void moveLeft(ByteBuffer buffer, int shiftBytes) {
        int length = buffer.limit();

        buffer.put(buffer.array(), shiftBytes, length - shiftBytes);
        buffer.position(0);
        buffer.limit(length - shiftBytes);
    }

    public static void moveRight(ByteBuffer buffer, int shiftBytes) {
        int length = buffer.limit();

        buffer.limit(length + shiftBytes);
        buffer.position(0);
        synchronized (mLocalBuf) {
            buffer.get(mLocalBuf, 0, length);
            buffer.position(shiftBytes);
            buffer.put(mLocalBuf, 0, length);
        }
        buffer.position(0);
    }

    public static void copy(ByteBuffer from, ByteBuffer to) {
        to.limit(from.limit() + 1); // java.nio.BufferOverflowException
        to.put(from.array(), 0, from.limit());
        to.limit(from.limit());
        to.position(0);
    }

    public static void put6bytes(ByteBuffer byteBuffer, long l) {
        // first byte
        byteBuffer.put((byte) ((l >>> (8 * 5)) & 0xFF));
        // second byte
        byteBuffer.put((byte) ((l >>> (8 * 4)) & 0xFF));
        // rest 4 bytes
        byteBuffer.putInt((int) (l & 0xFFFFFFFFL));
    }

    public static long get6bytes(ByteBuffer byteBuffer) {
        long l = 0;
        l |= (long) (byteBuffer.get() & 0xFF) << (8 * 5);
        l |= (long) (byteBuffer.get() & 0xFF) << (8 * 4);
        l |= byteBuffer.getInt() & 0xFFFFFFFFL;
        return l;
    }
}
