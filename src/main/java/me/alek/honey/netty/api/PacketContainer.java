package me.alek.honey.netty.api;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.utility.CallbackAction;
import me.alek.honey.netty.packet.PacketEventloopStateTimings;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.function.Consumer;

public interface PacketContainer<WP extends WrappedPacket<WP>> extends ModifiablePacketContainer {

    PacketEventloopStateTimings getEventloopStateTimings();

    void init(Object packet, CallbackAction<PacketContainer<WP>> postAction, PacketTypeEnum packetType);

    long getTime();

    void addCallbackAction(Consumer<PacketContainer<WP>> action);

    void invokeCallback();

    Channel getChannel();

    PacketTypeEnum getType();

    PacketState getState();

    WP getPacket();

    CallbackAction<PacketContainer<WP>> getPost();

    HoneyPlayer getPlayer();

    Object getHandle();

    String getName();

    boolean isCancelled();

    boolean isDumped();

    void cancel();

    void dump();

    boolean isHandledByServer();

    void setHandledByServer();
}
