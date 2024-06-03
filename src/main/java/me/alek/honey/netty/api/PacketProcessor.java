package me.alek.honey.netty.api;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.listener.PacketAdapter;
import me.alek.honey.netty.packet.PacketExecutor;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.List;
import java.util.function.Consumer;

public interface PacketProcessor {

    <WP extends WrappedPacket<WP>> PacketContainer<WP> read(Channel channel, HoneyPlayer player, Object packet, PacketContainer<WP> earlyInitializedPacket);

    <WP extends WrappedPacket<WP>> PacketContainer<WP> write(Channel channel, HoneyPlayer player, Object packet);

    void addPostAction(PacketTypeEnum packetType, Consumer<PacketContainer<? extends WrappedPacket<?>>> callback);

    void callDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf);

    void removeListener(PacketAdapter<?> packetAdapter, PacketTypeEnum... packetType);

    void removeListener(PacketAdapter<?> packetAdapter, List<PacketTypeEnum> packetType);

    void addPacketExecutor(PacketExecutor<? extends WrappedPacket<?>> packetExecutor, PacketTypeEnum packetType);

    <WP extends WrappedPacket<WP>> Runnable getPacketExecutorTask(PacketContainer<WP> packetContainer);

    void addListener(PacketAdapter<?> packetAdapter, ListenerPriority priority, PacketTypeEnum... packetType);

    void addListener(PacketAdapter<?> packetAdapter, ListenerPriority priority, List<PacketTypeEnum> packetType);

    void addDecodeListener(PacketAdapter<?> packetAdapter, ListenerPriority priority);

    <WP extends WrappedPacket<WP>> boolean isRegistered(PacketAdapter<WP> packetAdapter);

    void callListeners(HoneyPlayer player, Class<?> clazz, PacketContainer<?> packetContainer);
}

