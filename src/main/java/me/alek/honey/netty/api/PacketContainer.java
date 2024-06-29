package me.alek.honey.netty.api;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.netty.packet.PacketStateTimings;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.CallbackAction;

public interface PacketContainer<WP extends WrappedPacket<WP>> extends ModifiablePacketContainer, Cancellable {

    PacketStateTimings getEventloopStateTimings();

    void objectInitialized(Object packet, CallbackAction postAction, PacketType type);

    int getByteSize();

    long getTime();

    void addServerCallbackAction(CallbackAction.Action action);

    void invokeServerCallback();

    Channel getChannel();

    PacketType getType();

    PacketState getState();

    PacketState getProtocolState();

    WP getPacket();

    CallbackAction getPost();

    HoneyPlayer getPlayer();

    String getPlayerName();

    Object getRaw();

    boolean isCancelled();

    boolean isDumped();

    void cancel();

    void dump();

    boolean isHandledByServer();

    void setHandledByServer();
}
