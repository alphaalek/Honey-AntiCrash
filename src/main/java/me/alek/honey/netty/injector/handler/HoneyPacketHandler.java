package me.alek.honey.netty.injector.handler;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;


public class HoneyPacketHandler {

    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = new ThreadLocal<>();
    private volatile HoneyPlayer player;
    private PacketState protocolState;
    private WrappedEnumProtocol protocol;
    private String playerName;

    public HoneyPacketHandler(Channel channel) {
        updateProtocol(channel, PacketState.HANDSHAKE);
    }

    public void setPlayer(HoneyPlayer player) {
        this.player = player;
        this.playerName = player.name;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public PacketState getProtocolState() {
        return protocolState;
    }

    public WrappedEnumProtocol getProtocol() {
        return protocol;
    }

    public void updateProtocol(Channel channel, PacketState state) {
        this.protocolState = state;
        AttributeKey<?> protocolAttributeKey = WrappedEnumProtocol.getProtocolAttributeKey(channel, PacketBound.SERVER);
        protocol = new WrappedEnumProtocol(channel.attr(protocolAttributeKey).get());
    }

    public static ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> getPacketSendMarker() {
        return packetSendMarker;
    }

    private boolean isCancelled(PacketContainer<? extends WrappedPacket<?>> packetContainer) {
        if (packetContainer.isCancelled()) {
            if (ConfigurationOptions.PacketLogger.DEBUG_PACKETS_CANCELLED && packetContainer.getPlayer() != null) {
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + protocolState + ", " + packetContainer.getType().getBound() + ", " + packetContainer.getPlayer().name);
            }

            return true;
        }
        return false;
    }

    public synchronized boolean read(PacketContainer<? extends WrappedPacket<?>> packetContainer) {
        if (isCancelled(packetContainer)) {
            return false;
        }

        if (player != null && packetContainer.getPlayer() != null && packetContainer.getType() != PacketType.Play.Client.KEEP_ALIVE) {
            packetSendMarker.set(packetContainer);
        }

        return true;
    }

    public <WP extends WrappedPacket<WP>> boolean write(PacketContainer<WP> packetContainer) {
        return !isCancelled(packetContainer);
    }
}
