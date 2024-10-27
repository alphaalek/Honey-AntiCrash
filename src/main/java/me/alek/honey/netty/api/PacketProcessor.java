package me.alek.honey.netty.api;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.PacketExecutor;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;

import java.util.List;

public interface PacketProcessor {

    <WP extends WrappedPacket<WP>> PacketContainer<WP> read(HoneyPlayer player, Object objectPacket, PacketContainer<WP> packet);

    <WP extends WrappedPacket<WP>> PacketContainer<WP> write(HoneyPlayer player, Object objectPacket);

    void addPostAction(PacketType type, Callback.Action callback);

    <WP extends WrappedPacket<WP>> Runnable getPacketExecutorTask(PacketContainer<WP> packet);

    void removeListener(PacketListenerAdapter<?> listener, PacketContainer.State state, PacketType... types);

    void removeListener(PacketListenerAdapter<?> listener, PacketContainer.State state, List<PacketType> types);

    void addPacketExecutor(PacketType type, PacketExecutor<? extends WrappedPacket<?>> executor);

    void addListener(PacketListenerAdapter<?> listener, PacketContainer.State state, ListenerPriority priority, List<PacketType> types);

    <WP extends WrappedPacket<WP>> boolean isRegistered(PacketListenerAdapter<WP> listener, PacketContainer.State state);

    void callObjectPacketProcessListeners(HoneyPlayer player, PacketContainer<?> packet);

    void callBytePacketProcessListeners(HoneyPlayer player, PacketContainer<?> packet);
}

