package me.alek.honey.netty.packet;

import io.netty.channel.Channel;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.reflect.MethodInvoker;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PacketFactory {

    public static Map<PacketTypeEnum, MethodInvoker<Object>> WRAPPER_CACHE = new ConcurrentHashMap<>();

    public static synchronized WrappedPacket<?> createNewPacket(PacketTypeEnum type, Player player, Channel channel, Runnable postAction) {
        try {
            Object rawPacket = type.getNmsClass();

            return createWrappedPacket(type, rawPacket, player, channel, postAction);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static synchronized WrappedPacket<?> createWrappedPacket(PacketTypeEnum type, Object rawPacket, Player player, Channel channel, Runnable postAction) {
        return createWrappedPacket(type, rawPacket, new InternalPacketContainer<>(rawPacket, player, channel, postAction, type));
    }

    public static synchronized WrappedPacket<?> createWrappedPacket(PacketTypeEnum type, Object rawPacket, PacketContainer<?> packetContainer) {
        return (WrappedPacket<?>) getWrapperInvoker(type).invoke(null, rawPacket, packetContainer);
    }

    public static MethodInvoker<Object> getWrapperInvoker(PacketTypeEnum type) {
        return WRAPPER_CACHE.computeIfAbsent(type, packetType -> {
            Class<?> nmsClass = packetType.getNmsClass();

            return PacketWrapperInvokerFactory.createWrapperInvoker(packetType.getState(), nmsClass);
        });
    }
}
