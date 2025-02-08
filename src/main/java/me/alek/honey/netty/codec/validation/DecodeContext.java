package me.alek.honey.netty.codec.validation;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;

public interface DecodeContext {

    static DecodeContext from(PacketDataSerializer serializer, NetProtocol protocol, NetProtocol protocolCreated) {
        return new DecodeContext() {

            @Override
            public NetProtocol protocol() {
                return protocol;
            }

            @Override
            public NetProtocol protocolCreated() {
                return protocolCreated;
            }

            @Override
            public PacketDataSerializer serializer() {
                return serializer;
            }
        };
    }

    NetProtocol protocol();

    NetProtocol protocolCreated();

    PacketDataSerializer serializer();

    class BookEditContext<WP extends SourcedWrappedPacket<WP>> implements DecodeContext {

        public static <WP extends SourcedWrappedPacket<WP>> BookEditContext<WP> from(PacketDataSerializer buf, PacketContainer<WP> packet, boolean signing) {
            return new BookEditContext<>(buf, packet, signing);
        }

        private final boolean signing;
        private final PacketContainer<WP> packet;
        private final PacketDataSerializer buf;

        public BookEditContext(PacketDataSerializer buf, PacketContainer<WP> packet, boolean signing) {
            this.packet = packet;
            this.signing = signing;
            this.buf = buf;
        }

        public PacketContainer<WP> packet() {
            return this.packet;
        }

        @Override
        public NetProtocol protocol() {
            return this.packet.getProtocol();
        }

        @Override
        public NetProtocol protocolCreated() {
            return this.packet.getProtocolCreated();
        }

        @Override
        public PacketDataSerializer serializer() {
            return this.buf;
        }

        public boolean signing() {
            return this.signing;
        }
    }

    class StackContext implements DecodeContext {

        public static StackContext from(PacketDataSerializer buf, NetProtocol protocol, NetProtocol protocolCreated, int maxMissingBytes) {
            return new StackContext(buf, protocol, protocolCreated, maxMissingBytes);
        }

        private final int maxMissingBytes;
        private final NetProtocol protocol;
        private final NetProtocol protocolCreated;
        private final PacketDataSerializer buf;

        public StackContext(PacketDataSerializer buf, NetProtocol protocol, NetProtocol protocolCreatesd, int maxMissingBytes) {
            this.buf = buf;
            this.protocol = protocol;
            this.protocolCreated = protocolCreatesd;
            this.maxMissingBytes = maxMissingBytes;
        }

        public int maxMissingBytes() {
            return this.maxMissingBytes;
        }

        @Override
        public NetProtocol protocol() {
            return this.protocol;
        }

        @Override
        public NetProtocol protocolCreated() {
            return this.protocolCreated;
        }

        @Override
        public PacketDataSerializer serializer() {
            return this.buf;
        }
    }
}
