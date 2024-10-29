package me.alek.honey.netty.api;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.netty.packet.PacketLifecycleTimings;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;

public interface PacketContainer<WP extends WrappedPacket<WP>> extends Cancellable {

    enum PipelineState {
        INITIAL_BYTE, FINAL_BYTE, OBJECT
    }

    PacketLifecycleTimings getEventloopStateTimings();

    void initPostMinecraftDecode(Object packet, PacketType type);

    void initPreMinecraftDecode(PacketDataSerializer serializer);

    ObjectPacketStructure getStructure();

    long getTimeCreated();

    void addServerCallbackAction(Callback.Action action);

    void invokeServerCallback();

    Channel getChannel();

    PacketType getType();

    PacketState getState();

    PipelineState getProcessState();

    PacketState getProtocolState();

    ObjectWrappedPacket<WP> getObjectWrapped();

    ByteWrappedPacket<WP> getByteWrapped();

    WP getAnyWrapped();

    Callback getPost();

    HoneyPlayer getPlayer();

    String getPlayerName();

    boolean isCancelled();

    boolean isDumped();

    void cancel();

    void dump();

    void releaseIfAutoReleasable();
}
