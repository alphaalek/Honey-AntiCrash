package me.alek.honey.netty.api;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.PacketExecutor;
import me.alek.honey.netty.packet.PacketListener;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;

import java.util.List;

public interface PacketProcessor {

    <WP extends WrappedPacket<WP>> PacketContainer<WP> read(HoneyPlayer player, Object objectPacket, PacketContainer<WP> packet);

    <WP extends WrappedPacket<WP>> PacketContainer<WP> write(HoneyPlayer player, Object objectPacket);

    void addPostAction(PacketType type, Callback.Action callback);

    Callback getPostAction(PacketType type);

    <WP extends WrappedPacket<WP>> Runnable getPacketExecutorTask(PacketContainer<WP> packet);

    void removeListener(PacketListener<?> listener, PacketContainer.PipelineState state, PacketType... types);

    void removeListener(PacketListener<?> listener, PacketContainer.PipelineState state, List<PacketType> types);

    void addPacketExecutor(PacketType type, PacketExecutor<? extends WrappedPacket<?>> executor);

    void addListener(PacketListener<?> listener, PacketContainer.PipelineState state, ListenerPriority priority, List<PacketType> types);

    <WP extends WrappedPacket<WP>> boolean isRegistered(PacketListener<WP> listener, PacketContainer.PipelineState state);

    void callObjectPacketProcessListeners(HoneyPlayer player, PacketContainer<?> packet);

    void callInitialBytePacketProcessListeners(HoneyPlayer player, PacketContainer<?> packet);

    void callFinalBytePacketProcessListeners(HoneyPlayer player, PacketContainer<?> packet);
}

