package ru.esmukov.kpfu.lightningrodandroidvpnpoc;

import android.net.VpnService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kostya on 09/11/2016.
 */

public class SocatServerConnectionInfo {
    // c,IP,PORT,PROTOCOL
    private String mServerAddress = null;
    private Integer mServerPort = null;
    private SocatProtocol mServerProtocol = null;

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
                        this.mServerAddress = fields[1];
                        this.mServerPort = Integer.parseInt(fields[2]);
                        this.mServerProtocol = SocatProtocol.fromString(fields[3]);
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
                                new AddressMask(fields[1], Integer.parseInt(fields[2]))
                        );
                        break;
                    case 'd':
                        this.mLocalDnsServerAddressList.add(fields[1]);
                        break;
                    case 's':
                        this.mLocalSearchDomainList.add(fields[1]);
                        break;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Bad parameter: " + parameter);
            }
        }

        if (mServerAddress == null || mServerPort == null || mServerProtocol == null) {
            throw new IllegalArgumentException("Configuration must include server address."
                    + " Example: c,192.168.1.1,12312,tcp");
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

    public String getServerAddress() {
        return mServerAddress;
    }


    public Integer getServerPort() {
        return mServerPort;
    }

    public SocatProtocol getServerProtocol() {
        return mServerProtocol;
    }
}
