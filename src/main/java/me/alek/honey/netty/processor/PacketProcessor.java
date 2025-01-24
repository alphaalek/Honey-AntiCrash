package me.alek.honey.netty.processor;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.PacketExecutor;
import me.alek.honey.netty.packet.PacketListener;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;

import java.util.List;

public interface PacketProcessor {

    <WP extends SourcedWrappedPacket<WP>> PacketContainer<WP> readObjectPacket(HoneyPlayer player, Object objectPacket, PacketContainer<WP> packet);

    <WP extends SourcedWrappedPacket<WP>> PacketContainer<WP> writeObjectPacket(HoneyPlayer player, Object objectPacket);

    void addPostAction(PacketType type, Callback.Action callback);

    Callback getPostAction(PacketType type);

    <WP extends SourcedWrappedPacket<WP>> Runnable getPacketExecutorTask(PacketContainer<WP> packet);

    void removeListener(PacketListener<?> listener, PacketContainer.PipelineState state, PacketType... types);

    void removeListener(PacketListener<?> listener, PacketContainer.PipelineState state, List<PacketType> types);

    void addPacketExecutor(PacketType type, PacketExecutor<?> executor);

    void addListener(PacketListener<?> listener, PacketContainer.PipelineState state, ListenerPriority priority, List<PacketType> types);

    <WP extends SourcedWrappedPacket<WP>> boolean isRegistered(PacketListener<WP> listener, PacketContainer.PipelineState state);

    void callObjectPacketProcessListeners(HoneyPlayer player, PacketContainer<?> packet);

    void callInitialBytePacketProcessListeners(HoneyPlayer player, PacketContainer<?> packet);

    void callFinalBytePacketProcessListeners(HoneyPlayer player, PacketContainer<?> packet);
}

