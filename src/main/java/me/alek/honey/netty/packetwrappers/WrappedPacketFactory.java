package me.alek.honey.netty.packetwrappers;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;

import java.util.function.BiFunction;

public interface WrappedPacketFactory<TP extends SourcedWrappedPacket<WP>, WP extends SourcedWrappedPacket<WP>, T> {

    TP createPacket(T dependent, PacketContainer<WP> packet);

    Holder<?> holder = new Holder<>();

    @SuppressWarnings("unchecked")
    static <WP extends SourcedWrappedPacket<WP>> Holder<WP> acquireHolder() {
        return (Holder<WP>) holder;
    }

    class Holder<WP extends SourcedWrappedPacket<WP>> {

        public final ObjectWrappedPacketFactory<WP> OBJECT_FACTORY = ObjectWrappedPacketFactory.factoryFromSupplier(ObjectWrappedPacket::new);

        public final ByteWrappedPacketFactory<WP> BYTE_FACTORY = ByteWrappedPacketFactory.factoryFromSupplier(ByteInWrappedPacketImpl::new);
    }

    class ObjectWrappedPacketFactory<WP extends SourcedWrappedPacket<WP>> implements WrappedPacketFactory<ObjectWrappedPacket<WP>, WP, Object> {

        public static <WP extends SourcedWrappedPacket<WP>> ObjectWrappedPacketFactory<WP> factoryFromSupplier(BiFunction<Object, PacketContainer<WP>, ObjectWrappedPacket<WP>> function) {
            return new ObjectWrappedPacketFactory<>(function);
        }

        private final BiFunction<Object, PacketContainer<WP>, ObjectWrappedPacket<WP>> function;

        public ObjectWrappedPacketFactory(BiFunction<Object, PacketContainer<WP>, ObjectWrappedPacket<WP>> function) {
            this.function = function;
        }

        public ObjectWrappedPacket<WP> createPacket(Object handle, PacketContainer<WP> packet) {
            return this.function.apply(handle, packet);
        }
    }

    class ByteWrappedPacketFactory<WP extends SourcedWrappedPacket<WP>> implements WrappedPacketFactory<ByteInWrappedPacket<WP>, WP, PacketDataSerializer> {

        public static <WP extends SourcedWrappedPacket<WP>> ByteWrappedPacketFactory<WP> factoryFromSupplier(BiFunction<PacketDataSerializer, PacketContainer<WP>, ByteInWrappedPacket<WP>> function) {
            return new ByteWrappedPacketFactory<>(function);
        }

        private final BiFunction<PacketDataSerializer, PacketContainer<WP>, ByteInWrappedPacket<WP>> function;

        public ByteWrappedPacketFactory(BiFunction<PacketDataSerializer, PacketContainer<WP>, ByteInWrappedPacket<WP>> function) {
            this.function = function;
        }

        public ByteInWrappedPacket<WP> createPacket(PacketDataSerializer buf, PacketContainer<WP> packet) {
            return this.function.apply(buf, packet);
        }
    }
}
