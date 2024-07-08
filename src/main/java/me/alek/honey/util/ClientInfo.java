package me.alek.honey.util;

import me.alek.honey.util.protocol.NetProtocol;

public class ClientInfo {

    private final int protocolVersion;
    private final NetProtocol majorProtocol;
    private String brand;

    public ClientInfo(int protocolVersion) {
        this.protocolVersion = protocolVersion;
        this.majorProtocol = NetProtocol.getMajorFrom(protocolVersion);
        if (majorProtocol == null) {
            throw new IllegalArgumentException("Could not get major protocol from protocol version " + protocolVersion);
        }
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public NetProtocol getMajorProtocol() {
        return majorProtocol;
    }

    public String getBrand() {
        return brand;
    }
}
