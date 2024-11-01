package me.alek.honey.netty.packet;

import io.netty.buffer.ByteBuf;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacketFactory;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.NMS;

import java.util.logging.Level;

public class PacketFactory {

    public static Object createRawPacket(PacketType type) {
        if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_16_5)) {
            throw new UnsupportedOperationException();
        }

        try {
            return type.getNmsClass().getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static Object createPacket(PacketType type, ByteBuf serializer) {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_16_5) && serializer == null) {
            return createRawPacket(type);
        } else if (serializer == null) {
            throw new UnsupportedOperationException();
        }

        Object packetSerializer = new PacketDataSerializer(serializer).getSerializer();
        try {
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_16_5)) {
                Object raw = createRawPacket(type);
                if (raw != null) {
                    ReflectStructure.Provider.acquireStructure(type.getNmsClass())
                            .withTarget(raw).invokeMethod(null, 0, packetSerializer);
                }
                return raw;
            } else {
                return new ConstructorInvoker<>(type.getNmsClass(), NMS.getPacketDataSerializerClass())
                        .invoke(packetSerializer);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static synchronized <WP extends WrappedPacket<WP>, TP extends WrappedPacket<WP>> TP createNewPacket(WrappedPacketFactory<TP, WP, ?> factory, PacketType type, HoneyPlayer player) {
        return createNewPacket(factory, type, null, player);
    }

    public static synchronized <WP extends WrappedPacket<WP>, TP extends WrappedPacket<WP>> TP createNewPacket(WrappedPacketFactory<?, ?, ?> factory, PacketType type, ByteBuf serializer, HoneyPlayer player) {
        return createNewPacket(factory, type, player, serializer, null);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <WP extends WrappedPacket<WP>, TP extends WrappedPacket<WP>> TP createNewPacket(WrappedPacketFactory<?, ?, ?> factory, PacketType type, HoneyPlayer player, ByteBuf buf, Callback postAction) {
        try {
            if (factory instanceof WrappedPacketFactory.ObjectWrappedPacketFactory) {
                PacketDataSerializer serializer = new PacketDataSerializer(buf);
                return (TP) WrappedPacketFactoryCollection.initWrappedPacket(type, serializer, new InternalPacketContainer<WP>(PacketContainer.PipelineState.FINAL_BYTE, serializer, player, type, postAction));
            } else if (factory instanceof WrappedPacketFactory.ByteWrappedPacketFactory) {
                Object handle = createPacket(type, buf);
                return (TP) WrappedPacketFactoryCollection.initWrappedPacket(type, handle, new InternalPacketContainer<WP>(handle, player, type, postAction));
            }
        } catch (Exception ex) {
            HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "Error occurred when creating packet for type " + type, ex);
        }

        return null;
    }
}
