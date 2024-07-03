package me.alek.honey.netty.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.MethodInvoker;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;

import java.util.concurrent.atomic.AtomicReference;

public class PacketFactory {

    private static final AtomicReference<MethodInvoker<Void>> serializerApplyInvokerCache = new AtomicReference<>();

    public static Object createRawPacket(PacketType type) {
        if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_16_5)) {
            throw new UnsupportedOperationException();
        }

        try {
            return type.getNmsClass().newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static Object createPacket(PacketType type, ByteBuf serializer) {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_16_5)
                && !serializer.isReadable()) {
            return createRawPacket(type);
        }

        Object packetSerializer = new PacketDataSerializer(serializer).getSerializer();

        try {
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_16_5)) {
                Object raw = createRawPacket(type);
                ReflectStructureCache.acquireStructure(type.getNmsClass())
                        .withTarget(raw).invokeMethod(serializerApplyInvokerCache, 0, packetSerializer);
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

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketType type, HoneyPlayer player) {
        return createNewPacket(type, null, player);
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketType type, ByteBuf serializer, HoneyPlayer player) {
        return createNewPacket(type, player, serializer, player.channel, null);
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketType type, HoneyPlayer player, ByteBuf serializer, Channel channel, Callback postAction) {
        try {
            Object rawPacket;
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_16_5)) {
                rawPacket = createRawPacket(type);
            } else {
                rawPacket = createPacket(type, serializer);
            }

            return createWrappedPacket(type, rawPacket, player, channel, postAction);
        } catch (Exception ex) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred when creating packet for type " + type);
            ex.printStackTrace();
        }

        return null;
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createWrappedPacket(PacketType type, Object rawPacket, HoneyPlayer player, Channel channel, Callback postAction) {
        return PacketWrapperFactory.initWrappedPacket(type, rawPacket, (PacketContainer<WP>) new InternalPacketContainer<>(rawPacket, player, channel, postAction, type));
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createWrappedPacket(PacketType type, Object rawPacket, PacketContainer<WP> packetContainer) {
        return PacketWrapperFactory.initWrappedPacket(type, rawPacket, packetContainer);
    }
}
