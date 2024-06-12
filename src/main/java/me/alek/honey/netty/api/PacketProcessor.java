package me.alek.honey.netty.api;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.listener.PacketAdapter;
import me.alek.honey.netty.packet.PacketExecutor;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.List;
import java.util.function.Consumer;

public interface PacketProcessor {

    <WP extends WrappedPacket<WP>> PacketContainer<WP> read(Channel channel, HoneyPlayer player, HoneyPacketHandler handler, Object packet, PacketContainer<WP> earlyInitializedPacket);

    <WP extends WrappedPacket<WP>> PacketContainer<WP> write(Channel channel, HoneyPlayer player, HoneyPacketHandler handler, Object packet);

    void addPostAction(PacketType packetType, Consumer<PacketContainer<? extends WrappedPacket<?>>> callback);

    void callDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf);

    void removeListener(PacketAdapter<?> packetAdapter, PacketType... packetType);

    void removeListener(PacketAdapter<?> packetAdapter, List<PacketType> packetType);

    void addPacketExecutor(PacketType packetType, PacketExecutor<? extends WrappedPacket<?>> packetExecutor);

    <WP extends WrappedPacket<WP>> Runnable getPacketExecutorTask(PacketContainer<WP> packetContainer);

    void addListener(PacketAdapter<?> packetAdapter, ListenerPriority priority, PacketType... packetType);

    void addListener(PacketAdapter<?> packetAdapter, ListenerPriority priority, List<PacketType> packetType);

    void addDecodeListener(PacketAdapter<?> packetAdapter, ListenerPriority priority);

    <WP extends WrappedPacket<WP>> boolean isRegistered(PacketAdapter<WP> packetAdapter);

    void callListeners(HoneyPlayer player, Class<?> clazz, PacketContainer<?> packetContainer);
}

