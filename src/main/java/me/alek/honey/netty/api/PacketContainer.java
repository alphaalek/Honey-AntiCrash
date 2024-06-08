package me.alek.honey.netty.api;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.PacketEventLoopStateTimings;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;

import java.util.function.Consumer;

public interface PacketContainer<WP extends WrappedPacket<WP>> extends ModifiablePacketContainer {

    PacketEventLoopStateTimings getEventloopStateTimings();

    void init(Object packet, CallbackAction<PacketContainer<WP>> postAction, PacketTypeEnum packetType);

    long getTime();

    void addServerCallbackAction(Consumer<PacketContainer<WP>> action);

    void invokeServerCallback();

    Channel getChannel();

    PacketTypeEnum getType();

    Array<PacketState> getCommonStates();

    PacketState getState();

    PacketState getProtocolState();

    WP getPacket();

    CallbackAction<PacketContainer<WP>> getPost();

    HoneyPlayer getPlayer();

    String getPlayerName();

    Object getHandle();

    String getName();

    boolean isCancelled();

    boolean isDumped();

    void cancel();

    void dump();

    boolean isHandledByServer();

    void setHandledByServer();
}
