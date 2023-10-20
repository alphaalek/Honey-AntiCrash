package me.alek.honey.netty.packet;

import io.netty.channel.Channel;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.reflect.MethodInvoker;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PacketFactory {

    public static Map<PacketTypeEnum, MethodInvoker<Object>> WRAPPER_CACHE = new ConcurrentHashMap<>();

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketTypeEnum type, Player player) {
        return createNewPacket(type, player, NMSUtils.getChannel(player), null);
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketTypeEnum type, Player player, Channel channel, Runnable postAction) {
        try {
            Object rawPacket = type.getNmsClass().newInstance();

            return createWrappedPacket(type, rawPacket, player, channel, postAction);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createWrappedPacket(PacketTypeEnum type, Object rawPacket, Player player, Channel channel, Runnable postAction) {
        return createWrappedPacket(type, rawPacket, new InternalPacketContainer<>(rawPacket, player, channel, postAction, type));
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createWrappedPacket(PacketTypeEnum type, Object rawPacket, PacketContainer<?> packetContainer) {
        return (WrappedPacket<WP>) getWrapperInvoker(type).invoke(null, rawPacket, packetContainer);
    }

    public static MethodInvoker<Object> getWrapperInvoker(PacketTypeEnum type) {
        return WRAPPER_CACHE.computeIfAbsent(type, packetType -> {
            Class<?> nmsClass = packetType.getNmsClass();

            return PacketWrapperInvokerFactory.createWrapperInvoker(packetType.getState(), nmsClass);
        });
    }
}
