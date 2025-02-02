package me.alek.honey.netty.packet;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.packetwrappers.impl.login.WrappedLoginInLoginStart;
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

    PacketType getType();

    default PacketState getState() {
        return getType().getState();
    }

    default PacketState getProtocolState() {
        return getPacketHandler().getProtocolState(getType().getBound());
    }

    PipelineState getPipelineState();

    PipelineState getCreatedPipelineState();

    ObjectWrappedPacket<WP> getObjectWrapped();

    ByteInWrappedPacket<WP> getInitialByteWrapped();

    ByteInWrappedPacket<WP> getFinalByteWrapped();

    ByteInWrappedPacket<WP> getAnyByteWrapped();

    WP getAnyWrapped();

    WP getCurrentWrapped();

    Callback getPostAction();

    HoneyPlayer getPlayer();

    default HoneyPacketHandler getPacketHandler() {
        return getPlayer().packetHandler;
    }

    default Channel getChannel() {
        return getPlayer().channel;
    }

    default String getPlayerName() {
        String name = getPlayer().name;
        if (name.equals("Unknown")) {
            // if this is called before HELLO post action, the player name is not yet updated
            if (getType() == PacketTypes.Login.Client.HELLO) {
                return ((WrappedLoginInLoginStart) getAnyWrapped()).getUsername();
            }
        }
        return name;
    }

    boolean isCancelled();

    boolean isHandled();

    boolean isDumped();

    boolean isLogged();

    void cancel();

    void handle();

    void dump();

    void log();

    void releaseIfAutoReleasable();
}
