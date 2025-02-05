package me.alek.honey.netty.codec.validation;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;

public interface DecodeContext<WP extends SourcedWrappedPacket<WP>> {

    static <WP extends SourcedWrappedPacket<WP>> DecodeContext<WP> from(PacketContainer<WP> packet) {
        return () -> packet;
    }

    PacketContainer<WP> packet();

    class BookEditContext<WP extends SourcedWrappedPacket<WP>> implements DecodeContext<WP> {

        public static <WP extends SourcedWrappedPacket<WP>> BookEditContext<WP> of(PacketContainer<WP> packet, boolean signing) {
            return new BookEditContext<>(packet, signing);
        }

        private final boolean signing;
        private final PacketContainer<WP> packet;

        public BookEditContext(PacketContainer<WP> packet, boolean signing) {
            this.packet = packet;
            this.signing = signing;
        }

        @Override
        public PacketContainer<WP> packet() {
            return this.packet;
        }

        public boolean isSigning() {
            return this.signing;
        }
    }
}
