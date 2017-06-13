package ru.esmukov.kpfu.lightningrodandroidvpnpoc.osi.l3;

import java.io.IOException;

/**
 * Created by kostya on 13/06/2017.
 */

public class UnknownL3PacketException extends IOException {
    public UnknownL3PacketException() {
    }

    public UnknownL3PacketException(String detailMessage) {
        super(detailMessage);
    }
}
