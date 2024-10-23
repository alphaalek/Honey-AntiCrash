package me.alek.honey.netty.api;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.PacketExecutor;
import me.alek.honey.netty.packet.CancellableByteBuf;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;

import java.util.List;

public interface PacketProcessor {

    <WP extends WrappedPacket<WP>> PacketContainer<WP> read(Channel channel, HoneyPlayer player, Object packet, PacketContainer<WP> earlyInitializedPacket);

    <WP extends WrappedPacket<WP>> PacketContainer<WP> write(Channel channel, HoneyPlayer player, Object packet);

    void addPostAction(PacketType packetType, Callback.Action callback);

    <WP extends WrappedPacket<WP>> Runnable getPacketExecutorTask(PacketContainer<WP> packetContainer);

    void removeListener(PacketListenerAdapter<?> packetAdapter, PacketType... packetType);

    void removeListener(PacketListenerAdapter<?> packetAdapter, List<PacketType> packetType);

    void addPacketExecutor(PacketType packetType, PacketExecutor<? extends WrappedPacket<?>> packetExecutor);

    void addListener(PacketListenerAdapter<?> packetAdapter, ListenerPriority priority, PacketType... packetType);

    void addListener(PacketListenerAdapter<?> packetAdapter, ListenerPriority priority, List<PacketType> packetType);

    <WP extends WrappedPacket<WP>> boolean isRegistered(PacketListenerAdapter<WP> packetAdapter);

    void callListeners(HoneyPlayer player, PacketContainer<?> packetContainer);

    void callDecode(HoneyPlayer player, PacketType type, CancellableByteBuf byteBuf);
}

