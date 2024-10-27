package me.alek.honey.netty.packetwrappers;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;

import java.util.function.BiFunction;

public interface WrappedPacketFactory<TP extends WrappedPacket<WP>, WP extends WrappedPacket<WP>, T> {

    TP createPacket(T dependent, PacketContainer<WP> packet);

    Holder<?> holder = new Holder<>();

    @SuppressWarnings("unchecked")
    public static <WP extends WrappedPacket<WP>> Holder<WP> acquireHolder() {
        return (Holder<WP>) holder;
    }

    class Holder<WP extends WrappedPacket<WP>> {

        public final ObjectWrappedPacketFactory<WP> OBJECT_FACTORY = ObjectWrappedPacketFactory.factoryFromSupplier(ObjectWrappedPacket::new);

        public final ByteWrappedPacketFactory<WP> BYTE_FACTORY = ByteWrappedPacketFactory.factoryFromSupplier(ByteWrappedPacket::new);
    }

    class ObjectWrappedPacketFactory<WP extends WrappedPacket<WP>> implements WrappedPacketFactory<ObjectWrappedPacket<WP>, WP, Object> {

        public static <WP extends WrappedPacket<WP>> ObjectWrappedPacketFactory<WP> factoryFromSupplier(BiFunction<Object, PacketContainer<WP>, ObjectWrappedPacket<WP>> function) {
            return new ObjectWrappedPacketFactory<>(function);
        }

        private final BiFunction<Object, PacketContainer<WP>, ObjectWrappedPacket<WP>> function;

        public ObjectWrappedPacketFactory(BiFunction<Object, PacketContainer<WP>, ObjectWrappedPacket<WP>> function) {
            this.function = function;
        }

        public ObjectWrappedPacket<WP> createPacket(Object handle, PacketContainer<WP> packet) {
            return function.apply(handle, packet);
        }
    }

    class ByteWrappedPacketFactory<WP extends WrappedPacket<WP>> implements WrappedPacketFactory<ByteWrappedPacket<WP>, WP, PacketDataSerializer> {

        public static <WP extends WrappedPacket<WP>> ByteWrappedPacketFactory<WP> factoryFromSupplier(BiFunction<PacketDataSerializer, PacketContainer<WP>, ByteWrappedPacket<WP>> function) {
            return new ByteWrappedPacketFactory<>(function);
        }

        private final BiFunction<PacketDataSerializer, PacketContainer<WP>, ByteWrappedPacket<WP>> function;

        public ByteWrappedPacketFactory(BiFunction<PacketDataSerializer, PacketContainer<WP>, ByteWrappedPacket<WP>> function) {
            this.function = function;
        }

        public ByteWrappedPacket<WP> createPacket(PacketDataSerializer buf, PacketContainer<WP> packet) {
            return function.apply(buf, packet);
        }
    }
}
