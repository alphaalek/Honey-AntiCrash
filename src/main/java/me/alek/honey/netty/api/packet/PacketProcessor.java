package me.alek.honey.netty.api.packet;

import io.netty.channel.Channel;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;

import java.util.List;

public interface PacketProcessor {

    PacketContainer<? extends WrappedPacket<?>> read(Channel channel, Player player, Object packet);

    PacketContainer<? extends WrappedPacket<?>> write(Channel channel, Player player, Object packet);

    void removeListener(SyncPacketAdapter<?> packetAdapter, PacketTypeEnum... packetType);

    void removeListener(SyncPacketAdapter<?> packetAdapter, List<PacketTypeEnum> packetType);

    void addListener(SyncPacketAdapter<?> packetAdapter, ListenerPriority priority, PacketTypeEnum... packetType);

    void addListener(SyncPacketAdapter<?> packetAdapter, ListenerPriority priority, List<PacketTypeEnum> packetType);

    <WP extends WrappedPacket<WP>> boolean isRegistered(SyncPacketAdapter<WP> packetAdapter);

    void callListeners(Player player, Class<?> clazz, PacketContainer<?> packetContainer, boolean isRead);

    void errorListeners(Player player, Class<?> clazz, PacketContainer<?> packetContainer);

    void setPostAction(PacketTypeEnum packetType, Runnable postAction);

    void postRead();

    void postWrite();
}

