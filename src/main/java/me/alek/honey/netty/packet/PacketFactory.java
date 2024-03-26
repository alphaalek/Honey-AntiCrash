package me.alek.honey.netty.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.utility.CallbackAction;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.NMS;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.utility.Array;

import java.lang.invoke.MethodHandle;

public class PacketFactory {

    public static Array<MethodHandle> WRAPPER_CACHE = new Array<>();

    public static Object createRawPacket(PacketTypeEnum type) {
        if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_16_5)) throw new UnsupportedOperationException();

        try {
            return type.getNmsClass().newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static Object createRawPacket(PacketTypeEnum type, ByteBuf serializer) {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_16_5)) return createRawPacket(type);

        try {
            return type.getNmsClass().getDeclaredConstructor(NMS.getPacketDataSerializerClass())
                    .newInstance(new WrappedPacketDataSerializer(serializer).toNMSSerializer());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketTypeEnum type, HoneyPlayer player) {
        return createNewPacket(type, null, player);
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketTypeEnum type, ByteBuf serializer, HoneyPlayer player) {
        return createNewPacket(type, player, serializer, player.channel, null);
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketTypeEnum type, HoneyPlayer player, ByteBuf serializer, Channel channel, CallbackAction<PacketContainer<WP>> postAction) {
        try {
            Object rawPacket;
            if (serializer == null) {
                rawPacket = createRawPacket(type);
            } else {
                rawPacket = createRawPacket(type, serializer);
            }

            return createWrappedPacket(type, rawPacket, player, channel, postAction);
        } catch (Exception ex) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred when creating packet for type " + type);
            ex.printStackTrace();
        }
        return null;
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createWrappedPacket(PacketTypeEnum type, Object rawPacket, HoneyPlayer player, Channel channel, CallbackAction<PacketContainer<WP>> postAction) {
        return initWrappedPacket(type, rawPacket, new InternalPacketContainer<>(rawPacket, player, channel, postAction, type));
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createWrappedPacket(PacketTypeEnum type, Object rawPacket, PacketContainer<?> packetContainer) {
        return initWrappedPacket(type, rawPacket, packetContainer);
    }

    private static <WP extends WrappedPacket<WP>> WrappedPacket<WP> initWrappedPacket(PacketTypeEnum type, Object rawPacket, PacketContainer<?> packetContainer) {
        MethodHandle wrapperInvoker = getWrapperInvoker(type);
        if (wrapperInvoker == null) return null;

        try {
            Object packet = wrapperInvoker.invokeWithArguments(new Object[]{rawPacket, packetContainer});
            return (WrappedPacket<WP>) packet;
        } catch (Throwable ex) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred when creating wrapped packet for type " + type);
            ex.printStackTrace();
            return null;
        }
    }

    public static MethodHandle getWrapperInvoker(PacketTypeEnum type) {
        return WRAPPER_CACHE.get(type.getPacketId(), () -> {
            Class<?> nmsClass = type.getNmsClass();

            return PacketWrapperInvokerFactory.createWrapperInvoker(type.getState(), nmsClass);
        });
    }
}
