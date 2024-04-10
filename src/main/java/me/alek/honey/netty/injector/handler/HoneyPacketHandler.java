package me.alek.honey.netty.injector.handler;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.util.Optional;

public class HoneyPacketHandler {

    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = new ThreadLocal<>();
    private volatile HoneyPlayer player;
    private PacketState protocolState = PacketState.UNKNOWN;
    private String playerName;

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

    public void setProtocolState(PacketState state) {
        this.protocolState = state;
    }

    public static ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> getPacketSendMarker() {
        return packetSendMarker;
    }

    private boolean isCancelled(PacketContainer<? extends WrappedPacket<?>> packetContainer) {
        if (packetContainer.isCancelled()) {
            if (ConfigurationOptions.PacketLogger.DEBUG_PACKETS_CANCELLED && packetContainer.getPlayer() != null) {
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + protocolState + "," + packetContainer.getType().getBound() + ": " + packetContainer.getPlayer().name);
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
