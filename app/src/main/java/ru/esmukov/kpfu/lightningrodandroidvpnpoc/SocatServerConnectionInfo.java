package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

import android.net.VpnService;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.PacketFilter;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l3.L3PacketFilter;
import ru.esmukov.kpfu.lightningrodandroidvpnpoc.packetfilter.l2.L2ToL3PacketFilter;

/**
 * Created by kostya on 09/11/2016.
 */

public class SocatServerConnectionInfo {
    // c,IP,PORT,PROTOCOL
    // l,PORT,tcp
    private RemoteConnectionInfo mRemoteConnectionInfo = null;

    // m,MTU
    private Short mMtu = null;

    // a,IP,MASK
    private List<AddressMask> mLocalInterfaceAddressList = new ArrayList<>();
    // r,IP,MASK
    private List<AddressMask> mLocalRouteAddressList = new ArrayList<>();
    // d,IP
    private List<String> mLocalDnsServerAddressList = new ArrayList<>();
    // s,DOMAIN
    private List<String> mLocalSearchDomainList = new ArrayList<>();

    // n,VPN_SERVICE_NAME (like if dev name)
    private String mVpnServiceName = "socat0";

    // o,pi
    private boolean mPacketInfo = false;

    // Tap -- L2 tunnel.
    // Tun (default) -- L3 tunnel -- the same level as the VpnService.
    // o,tap || o,tun
    private boolean mIsTap = false;

    /**
     * @param configuration Example: "c,192.168.1.1,12312,tcp a,10.123.123.2,24 r,10.123.123.0,24"
     */
    public SocatServerConnectionInfo(String configuration) {
        for (String parameter : configuration.split(" ")) {
            parameter = parameter.trim();

            if (parameter.isEmpty())
                continue;

            String[] fields = parameter.split(",");
            try {
                switch (fields[0].charAt(0)) {
                    case 'c':
                        this.mRemoteConnectionInfo = new ConnectRemoteConnectionInfo(
                                fields[1], Integer.parseInt(fields[2]), SocatProtocol.fromString(fields[3])
                        );
                        break;
                    case 'l':
                        this.mRemoteConnectionInfo = new ListenRemoteConnectionInfo(
                                Integer.parseInt(fields[1]), SocatProtocol.fromString(fields[2])
                        );
                        break;
                    case 'm':
                        this.mMtu = Short.parseShort(fields[1]);
                        break;
                    case 'a':
                        this.mLocalInterfaceAddressList.add(
                                new AddressMask(fields[1], Integer.parseInt(fields[2]))
                        );
                        break;
                    case 'r':
                        this.mLocalRouteAddressList.add(
                                AddressMask.networkAddressMask(fields[1], Integer.parseInt(fields[2]))
                        );
                        break;
                    case 'd':
                        this.mLocalDnsServerAddressList.add(fields[1]);
                        break;
                    case 's':
                        this.mLocalSearchDomainList.add(fields[1]);
                        break;
                    case 'n':
                        this.mVpnServiceName = fields[1];
                        break;
                    case 'o':
                        if ("pi".equalsIgnoreCase(fields[1]))
                            mPacketInfo = true;
                        if ("no_pi".equalsIgnoreCase(fields[1]))
                            mPacketInfo = false;

                        if ("tap".equalsIgnoreCase(fields[1]))
                            mIsTap = true;
                        if ("tun".equalsIgnoreCase(fields[1]))
                            mIsTap = false;
                        break;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Bad parameter: " + parameter, e);
            }
        }

        if (mRemoteConnectionInfo == null) {
            throw new IllegalArgumentException("Configuration must include remote connection info."
                    + " Examples: `c,192.168.1.1,12312,tcp`, `l,12312,tcp`.");
        }
    }

    public void applyToVpnServiceBuilder(VpnService.Builder builder) {
        if (mMtu != null)
            builder.setMtu(mMtu);

        for (AddressMask interfaceAddress : mLocalInterfaceAddressList) {
            builder.addAddress(interfaceAddress.getAddress(), interfaceAddress.getPrefixLength());
        }

        for (AddressMask routeAddress : mLocalRouteAddressList) {
            builder.addRoute(routeAddress.getAddress(), routeAddress.getPrefixLength());
        }

        for (String dnsServerAddress : mLocalDnsServerAddressList) {
            builder.addDnsServer(dnsServerAddress);
        }

        for (String searchDomain : mLocalSearchDomainList) {
            builder.addSearchDomain(searchDomain);
        }
    }

    public String getVpnServiceName() {
        return mVpnServiceName;
    }

    public RemoteConnectionInfo getRemoteConnectionInfo() {
        return mRemoteConnectionInfo;
    }

    public PacketFilter createNewPacketFilter() {
        if (mIsTap) {
            return new L2ToL3PacketFilter(mPacketInfo);
        } else {
            return new L3PacketFilter(mPacketInfo);
        }
    }

    public interface RemoteConnectionInfo {
        SocatProtocol getSocatProtocol();

        boolean isConnectSocket();

        InetSocketAddress getInetSocketAddress();
    }

    public static class ConnectRemoteConnectionInfo implements RemoteConnectionInfo {
        private String mAddress;
        private int mPort;
        private SocatProtocol mProtocol;

        public ConnectRemoteConnectionInfo(String mAddress, Integer mPort, SocatProtocol mProtocol) {
            if (mAddress == null || mPort == null || mProtocol == null) {
                throw new IllegalArgumentException("Bad 'c' option params."
                        + " Example: c,192.168.1.1,12312,tcp");
            }
            this.mProtocol = mProtocol;
            this.mAddress = mAddress;
            this.mPort = mPort;
        }

        @Override
        public SocatProtocol getSocatProtocol() {
            return mProtocol;
        }

        @Override
        public boolean isConnectSocket() {
            return true;
        }

        @Override
        public InetSocketAddress getInetSocketAddress() {
            return new InetSocketAddress(mAddress, mPort);
        }
    }

    public static class ListenRemoteConnectionInfo implements RemoteConnectionInfo {
        private int mPort;
        private SocatProtocol mProtocol;

        public ListenRemoteConnectionInfo(Integer mPort, SocatProtocol mProtocol) {
            if (mPort == null || mProtocol == null) {
                throw new IllegalArgumentException("Bad 'l' option params."
                        + " Example: l,12312,tcp");
            }
            this.mPort = mPort;
            this.mProtocol = mProtocol;
        }

        @Override
        public SocatProtocol getSocatProtocol() {
            return mProtocol;
        }

        @Override
        public boolean isConnectSocket() {
            return false;
        }

        @Override
        public InetSocketAddress getInetSocketAddress() {
            return new InetSocketAddress(mPort);
        }
    }

}
