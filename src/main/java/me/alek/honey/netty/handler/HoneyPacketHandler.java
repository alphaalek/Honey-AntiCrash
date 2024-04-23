package me.alek.honey.netty.handler;

import io.netty.channel.Channel;
import io.netty.handler.codec.DecoderException;
import io.netty.util.AttributeKey;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.Optional;


public class HoneyPacketHandler {

    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = new ThreadLocal<>();
    private final Channel channel;
    private volatile HoneyPlayer player;
    private PacketState protocolState = PacketState.HANDSHAKE;
    private WrappedEnumProtocol protocol;
    private String playerName;

    public HoneyPacketHandler(Channel channel) {
        this.channel = channel;
        updateProtocol();
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

    public void updateProtocol() {
        if (WrappedEnumProtocol.getEnumProtocolDelegateOrdinal(channel, PacketBound.SERVER) != this.protocolState.ordinal()
                && channel.pipeline().get("pre-resolved-packet-handler") == null) {
            Object protocol = NMS.getEnumProtocolClass().getEnumConstants()[this.protocolState.ordinal()];
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
                Map<Object, Object> delegatedDirectionPacketFlowMap = ReflectStructureCache.acquireStructure(protocol.getClass())
                        .<Map<Object, Object>>withWeakType(Map.class).withTarget(protocol).readField(0);
                protocol = delegatedDirectionPacketFlowMap.get(PacketBound.SERVER.getNMSWrapperType());
            }
            this.protocol = new WrappedEnumProtocol(protocol);
        } else {
            this.protocol = WrappedEnumProtocol.getEnumProtocol(channel, PacketBound.SERVER);
        }

        if (!ConfigurationOptions.Decoder.USE_DEFAULT) {
            channel.attr(WrappedEnumProtocol.getProtocolAttributeKey(channel, PacketBound.SERVER)).set(this.protocol);
        }
    }

    public void setProtocolState(PacketState state) {
        this.protocolState = state;
        updateProtocol();
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

        ProtocolSwapHandler.handleProtocolSwap(packetContainer);
        packetSendMarker.set(packetContainer);

        return true;
    }

    public <WP extends WrappedPacket<WP>> boolean write(PacketContainer<WP> packetContainer) {
        Bukkit.getLogger().info("Write " + packetContainer.getType());
        return !isCancelled(packetContainer);
    }
}
