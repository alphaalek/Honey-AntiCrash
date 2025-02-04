package me.alek.honey.util;

import me.alek.honey.util.protocol.NetProtocol;

import java.util.Optional;

public interface ClientInfo {

    int PINGING_PROTOCOL_VERSION = -1;

    ClientInfo NON_EXISTENT = new ClientInfo() {

        @Override
        public int getProtocolVersion() {
            return -1;
        }

        @Override
        public NetProtocol getProtocol() {
            return NetProtocol.getProtocol();
        }

        @Override
        public String getBrand() {
            throw new UnsupportedOperationException("This client info does not belong to a real client");
        }

        @Override
        public void setBrand(String brand) {
            throw new UnsupportedOperationException("Can't set brand for a non existent client");
        }
    };

    class Default implements ClientInfo {

        public static final String VANILLA = "vanilla";

        private final int protocolVersion;
        private final NetProtocol majorProtocol;
        private String brand;

        public Default(int protocolVersion) {
            this.protocolVersion = protocolVersion;
            this.majorProtocol = NetProtocol.getMajorProtocolFrom(protocolVersion);
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

        public NetProtocol getProtocol() {
            return majorProtocol;
        }

        public String getBrand() {
            return Optional.ofNullable(brand).orElse(VANILLA);
        }
    }

    void setBrand(String brand);

    int getProtocolVersion();

    NetProtocol getProtocol();

    String getBrand();
}
