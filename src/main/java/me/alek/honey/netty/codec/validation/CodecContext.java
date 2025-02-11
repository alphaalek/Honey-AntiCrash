package me.alek.honey.netty.codec.validation;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;

public interface CodecContext {

    static CodecContext from(PacketDataSerializer serializer, NetProtocol protocol, NetProtocol protocolCreated) {
        return new CodecContext() {

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

    interface Transformer extends CodecContext {

        int maxMissingBytes();
    }

    NetProtocol protocol();

    NetProtocol protocolCreated();

    PacketDataSerializer serializer();

    class BookValidationContext<WP extends SourcedWrappedPacket<WP>> implements CodecContext {

        public static <WP extends SourcedWrappedPacket<WP>> BookValidationContext<WP> from(PacketDataSerializer serializer, PacketContainer<WP> packet, boolean signing) {
            return new BookValidationContext<>(serializer, packet, signing);
        }

        private final boolean signing;
        private final PacketContainer<WP> packet;
        private final PacketDataSerializer serializer;

        public BookValidationContext(PacketDataSerializer serializer, PacketContainer<WP> packet, boolean signing) {
            this.serializer = serializer;
            this.packet = packet;
            this.signing = signing;
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
            return this.serializer;
        }

        public boolean signing() {
            return this.signing;
        }
    }

    class StackContextImpl implements CodecContext.Transformer {

        public static class BookEditing extends StackContextImpl {

            public static BookEditing from(PacketDataSerializer serializer, NetProtocol protocol, NetProtocol protocolCreated, int maxMissingBytes, String playerName, boolean signing) {
                return new BookEditing(serializer, protocol, protocolCreated, maxMissingBytes, playerName, signing);
            }

            private final String playerName;
            private final boolean signing;

            public BookEditing(PacketDataSerializer serializer, NetProtocol protocol, NetProtocol protocolCreated, int maxMissingBytes, String playerName, boolean signing) {
                super(serializer, protocol, protocolCreated, maxMissingBytes);
                this.playerName = playerName;
                this.signing = signing;
            }

            public String getPlayerName() {
                return this.playerName;
            }

            public boolean isSigning() {
                return this.signing;
            }
        }

        public static StackContextImpl from(PacketDataSerializer serializer, NetProtocol protocol, NetProtocol protocolCreated, int maxMissingBytes) {
            return new StackContextImpl(serializer, protocol, protocolCreated, maxMissingBytes);
        }

        private final int maxMissingBytes;
        private final NetProtocol protocol;
        private final NetProtocol protocolCreated;
        private final PacketDataSerializer serializer;

        public StackContextImpl(PacketDataSerializer serializer, NetProtocol protocol, NetProtocol protocolCreated, int maxMissingBytes) {
            this.serializer = serializer;
            this.protocol = protocol;
            this.protocolCreated = protocolCreated;
            this.maxMissingBytes = maxMissingBytes;
        }

        @Override
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
            return this.serializer;
        }
    }
}
