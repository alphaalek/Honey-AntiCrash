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
import me.alek.honey.util.protocol.NetProtocol;

public interface PacketContainer<WP extends WrappedPacket<WP>> extends Cancellable {

    enum PipelineState {
        INITIAL_BYTE, FINAL_BYTE, OBJECT
    }

    PacketLifecycleTimings getEventloopStateTimings();

    void handleRewritten(PacketType type, PipelineState state);

    void callPostMinecraftDecode(Object packet, PacketType type);

    void callPreMinecraftDecode(PacketDataSerializer serializer);

    void callLateInitialByteListeners(PacketDataSerializer serializer);

    NetProtocol getProtocolByPacketState();

    ObjectPacketStructure getStructure();

    long getTimeCreated();

    void addServerCallbackAction(Callback.Action action);

    void invokeServerCallback();

    Channel getChannel();

    PacketType getType();

    PacketState getState();

    PipelineState getPipelineState();

    PacketState getProtocolState();

    ObjectWrappedPacket<WP> getObjectWrapped();

    ByteWrappedPacket<WP> getInitialByteWrapped();

    ByteWrappedPacket<WP> getFinalByteWrapped();

    WP getAnyWrapped();

    Callback getPostAction();

    HoneyPlayer getPlayer();

    String getPlayerName();

    boolean isCancelled();

    boolean isDumped();

    void cancel();

    void dump();

    void releaseIfAutoReleasable();
}
