package me.alek.honey.netty.packet;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ObjectPacketStructure;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;

public interface PacketContainer<WP extends SourcedWrappedPacket<WP>> extends Cancellable {

    enum PipelineState {
        INITIAL_BYTE, FINAL_BYTE, OBJECT;
    }

    PacketLifecycleTimings getEventloopStateTimings();

    void handleRewritten(PacketType type, PipelineState state);

    void callPostMinecraftDecode(Object packet, PacketType type);

    void callPreMinecraftDecode(PacketDataSerializer serializer);

    void callLateInitialByteListeners(PacketDataSerializer serializer);

    NetProtocol getProtocol();

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

    ByteInWrappedPacket<WP> getInitialByteWrapped();

    ByteInWrappedPacket<WP> getFinalByteWrapped();

    WP getAnyWrapped();

    WP getCurrentWrapped();

    Callback getPostAction();

    HoneyPlayer getPlayer();

    String getPlayerName();

    boolean isCancelled();

    boolean isDumped();

    boolean isLogged();

    void cancel();

    void dump();

    void log();

    void releaseIfAutoReleasable();
}
