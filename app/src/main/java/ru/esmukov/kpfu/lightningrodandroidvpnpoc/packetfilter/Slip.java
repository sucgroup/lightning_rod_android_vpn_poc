package ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter;

import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 28/07/2017.
 */

public class Slip {
    private static final String TAG = "Slip";

    // ByteBuffer.get/ByteBuffer.put are VERY slow. copying to a byte array,
    // modifying it and putting it back is a lot faster.
    private final static byte[] mLocalBuf = new byte[2<<17];

    // SLIP special character codes
    // https://tools.ietf.org/html/rfc1055
    public static final byte END = (byte) 0300;    /* indicates end of packet */
    public static final byte ESC = (byte) 0333;    /* indicates byte stuffing */
    public static final byte ESC_END = (byte) 0334;    /* ESC ESC_END means END data byte */
    public static final byte ESC_ESC = (byte) 0335;    /* ESC ESC_ESC means ESC data byte */


    protected static boolean slipStrip(ByteBuffer buffer, int prevPos) {
        // VpnService doesn't understand anything but IPv4 anyway,
        // so lets just strip SLIP data and rely on IPv4 headers
        // to determine packet length.
        // todo: actually use SLIP boundaries after switching to single syscalls
        // when working with tunnel

        int bufsize = buffer.position();
        if (bufsize > 0 && buffer.get(bufsize - 1) == ESC) {
            // we need to get the corresponding pair first (ESC_END/ESC_ESC).
            // so don't do anything for now and read more data.
            return false;
        }
        if (bufsize == prevPos)
            return true;

        synchronized (mLocalBuf) {

            if (mLocalBuf.length < bufsize)
                throw new RuntimeException("SLIP buffer is too small");

            buffer.position(0);
            buffer.get(mLocalBuf, 0, bufsize);

            int posRaw;
            int pos = prevPos;  // transformed
            for (posRaw = prevPos; posRaw < bufsize; ) {
                switch (mLocalBuf[posRaw]) {
                    case END:
                        posRaw++;
                        break;
                    case ESC:
                        switch (mLocalBuf[posRaw + 1]) {
                            case ESC_END:
                                mLocalBuf[pos] = END;
                                break;
                            case ESC_ESC:
                                mLocalBuf[pos] = ESC;
                                break;
                            default:  // not valid SLIP actually. write some junk
                                Log.w(TAG, String.format(
                                        "Invalid data after SLIP_ESC: %x. " +
                                                "Skipping ESC byte.", mLocalBuf[posRaw + 1]));
                                mLocalBuf[pos] = mLocalBuf[posRaw + 1];
                                break;
                        }
                        pos++;
                        posRaw += 2;
                        break;
                    default:
                        mLocalBuf[pos] = mLocalBuf[posRaw];
                        pos++;
                        posRaw++;
                        break;
                }
            }
            buffer.position(0);
            buffer.put(mLocalBuf, 0, pos);
            buffer.position(pos);
        }

        return true;
    }

    protected static void slipEncodeFrame(ByteBuffer buffer) {
        int bufferCapacity = mLocalBuf.length;  // buffer.capacity();
        int frameLength = buffer.limit();

        synchronized (mLocalBuf) {

            if (mLocalBuf.length < frameLength)
                throw new RuntimeException("SLIP buffer is too small");

            buffer.position(0);
            // SLIP encoding never shrinks the frame: it only grows it.
            // Here's a hack: move the frame to the tail of the buffer
            // so we can grow the packet when escaping END/ESC bytes.
            buffer.get(mLocalBuf, bufferCapacity - frameLength, frameLength);

            int posTransformed = 0;
            int pos;
            for (pos = bufferCapacity - frameLength; pos < bufferCapacity; pos++) {
                if (posTransformed + 2 >= pos) {
                    Log.w(TAG, String.format(
                            "Packets overlap on applying SLIP transformations to a packet " +
                                    "read from tun. This packet is skipped. Consider increasing " +
                                    "buffer length. (%d, %d)", frameLength, bufferCapacity));
                    buffer.limit(0);
                    return;
                }

                switch (mLocalBuf[pos]) {
                    case END:
                        mLocalBuf[posTransformed] = ESC;
                        mLocalBuf[posTransformed + 1] = ESC_END;
                        posTransformed += 2;
                        break;
                    case ESC:
                        mLocalBuf[posTransformed] = ESC;
                        mLocalBuf[posTransformed + 1] = ESC_ESC;
                        posTransformed += 2;
                        break;
                    default:
                        mLocalBuf[posTransformed] = mLocalBuf[pos];
                        posTransformed++;
                        break;
                }
            }
            mLocalBuf[posTransformed] = END;
            posTransformed++;

            buffer.position(0);
            buffer.limit(posTransformed);
            buffer.put(mLocalBuf, 0, posTransformed);
            buffer.position(0);
        }
    }
}
