package me.alek.honey.netty.packet;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.MethodInvoker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PacketFactory {

    public static Map<PacketTypeEnum, MethodInvoker<Object>> WRAPPER_CACHE = new ConcurrentHashMap<>();

    public static Object createRawPacket(PacketTypeEnum type) {
        try {
            return type.getNmsClass().newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketTypeEnum type, HoneyPlayer player) {
        return createNewPacket(type, player, player.channel, null);
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketTypeEnum type, HoneyPlayer player, Channel channel, Runnable postAction) {
        try {
            Object rawPacket = type.getNmsClass().newInstance();

            return createWrappedPacket(type, rawPacket, player, channel, postAction);
        } catch (Exception ex) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred when creating packet for type " + type);
            ex.printStackTrace();
        }
        return null;
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createWrappedPacket(PacketTypeEnum type, Object rawPacket, HoneyPlayer player, Channel channel, Runnable postAction) {
        return initWrappedPacket(type, rawPacket, new InternalPacketContainer<>(rawPacket, player, channel, postAction, type));
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createWrappedPacket(PacketTypeEnum type, Object rawPacket, PacketContainer<?> packetContainer) {
        return initWrappedPacket(type, rawPacket, packetContainer);
    }

    private static <WP extends WrappedPacket<WP>> WrappedPacket<WP> initWrappedPacket(PacketTypeEnum type, Object rawPacket, PacketContainer<?> packetContainer) {
        MethodInvoker<Object> wrapperInvoker = getWrapperInvoker(type);
        if (wrapperInvoker == null) return null;

        try {
            return (WrappedPacket<WP>) wrapperInvoker.invokeWithException(null, rawPacket, packetContainer);
        } catch (Exception ex) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred when creating wrapped packet for type " + type);
            ex.printStackTrace();
            return null;
        }
    }

    public static MethodInvoker<Object> getWrapperInvoker(PacketTypeEnum type) {
        return WRAPPER_CACHE.computeIfAbsent(type, packetType -> {
            Class<?> nmsClass = packetType.getNmsClass();

            return PacketWrapperInvokerFactory.createWrapperInvoker(packetType.getState(), nmsClass);
        });
    }
}
