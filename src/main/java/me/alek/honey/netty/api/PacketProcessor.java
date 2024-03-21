package me.alek.honey.netty.api;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.List;

public interface PacketProcessor {

    PacketContainer<? extends WrappedPacket<?>> read(Channel channel, HoneyPlayer player, Object packet, long readStart, PacketContainer<? extends WrappedPacket<?>> earlyInitializedPacket);

    PacketContainer<? extends WrappedPacket<?>> read(Channel channel, HoneyPlayer player, Object packet, PacketContainer<? extends WrappedPacket<?>> earlyInitializedPacket);

    PacketContainer<? extends WrappedPacket<?>> write(Channel channel, HoneyPlayer player, Object packet);

    void callDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf);

    void removeListener(SyncPacketAdapter<?> packetAdapter, PacketTypeEnum... packetType);

    void removeListener(SyncPacketAdapter<?> packetAdapter, List<PacketTypeEnum> packetType);

    void addListener(SyncPacketAdapter<?> packetAdapter, ListenerPriority priority, PacketTypeEnum... packetType);

    void addListener(SyncPacketAdapter<?> packetAdapter, ListenerPriority priority, List<PacketTypeEnum> packetType);

    void addDecodeListener(SyncPacketAdapter<?> packetAdapter, ListenerPriority priority);

    <WP extends WrappedPacket<WP>> boolean isRegistered(SyncPacketAdapter<WP> packetAdapter);

    void callListeners(HoneyPlayer player, Class<?> clazz, PacketContainer<?> packetContainer, boolean isRead);

    void setPostAction(PacketTypeEnum packetType, Runnable postAction);

    void postRead();

    void postWrite();
}

