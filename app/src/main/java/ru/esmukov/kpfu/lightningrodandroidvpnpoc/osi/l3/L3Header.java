package ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3;

import java.nio.ByteBuffer;

/**
 * Created by kostya on 06/06/2017.
 */

public interface L3Header {

    int getMinimumHeaderLength();

    int getTotalLength(ByteBuffer packet);
}
