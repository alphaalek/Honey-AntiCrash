package me.alek.honey.netty.handler;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.util.Map;


public class HoneyPacketHandler {

    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = new ThreadLocal<>();
    private final ProtocolSwapHandler protocolSwapHandler = new ProtocolSwapHandler();
    private final Channel channel;
    private volatile HoneyPlayer player;
    private PacketState clientboundProtocolState = PacketState.HANDSHAKE;
    private PacketState serverboundProtocolState = PacketState.HANDSHAKE;
    private WrappedEnumProtocol clientboundProtocol;
    private WrappedEnumProtocol serverboundProtocol;
    private String playerName;

    public HoneyPacketHandler(Channel channel) {
        this.channel = channel;
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

    public PacketState getProtocolState(PacketBound bound) {
        return bound == PacketBound.CLIENT ? clientboundProtocolState : serverboundProtocolState;
    }

    public WrappedEnumProtocol getProtocol(PacketBound bound) {
        return bound == PacketBound.CLIENT ? clientboundProtocol : serverboundProtocol;
    }

    public void setProtocol(PacketBound bound, WrappedEnumProtocol protocol) {
        if (bound == PacketBound.CLIENT) {
            this.clientboundProtocol = protocol;
        } else if (bound == PacketBound.SERVER) {
            this.serverboundProtocol = protocol;
        }
    }

    public void updateProtocol(PacketBound bound) {
        Object protocol = NMS.getEnumProtocolClass().getEnumConstants()[getProtocolState(bound).ordinal()];
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_2)) {
            Map<Object, Object> delegatedDirectionPacketFlowMap = ReflectStructureCache.acquireStructure(NMS.getEnumProtocolClass())
                    .<Map<Object, Object>>withWeakType(Map.class).withTarget(protocol).readField(0);
            protocol = delegatedDirectionPacketFlowMap.get(bound.getNMSWrapperType());
        }
        setProtocol(bound, new WrappedEnumProtocol(protocol));

        if (channel.pipeline().get("pre-resolved-packet-handler") == null && !ConfigurationOptions.Decoder.USE_DEFAULT) {
            Attribute<Object> protocolAttribute = channel.attr(WrappedEnumProtocol.getProtocolAttributeKey(channel, bound));
            protocolAttribute.set(getProtocol(bound).getHandle());
        }
    }

    public void setProtocolState(PacketBound bound, PacketState state) {
        if (bound == PacketBound.SERVER) {
            serverboundProtocolState = state;
            updateProtocol(PacketBound.SERVER);
        } else if (bound == PacketBound.CLIENT) {
            clientboundProtocolState = state;
            updateProtocol(PacketBound.CLIENT);
        } else {
            serverboundProtocolState = state;
            clientboundProtocolState = state;
            updateProtocol(PacketBound.SERVER);
            updateProtocol(PacketBound.CLIENT);
        }
    }

    public static ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> getPacketSendMarker() {
        return packetSendMarker;
    }

    private boolean isCancelled(PacketContainer<? extends WrappedPacket<?>> packetContainer) {
        if (packetContainer.isCancelled()) {
            if (ConfigurationOptions.PacketLogger.DEBUG_PACKETS_CANCELLED && packetContainer.getPlayer() != null) {
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + getProtocolState(packetContainer.getType().getBound()) + ", " + packetContainer.getType().getBound() + ", " + packetContainer.getPlayer().name);
            }

            return true;
        }
        return false;
    }

    public synchronized boolean read(PacketContainer<? extends WrappedPacket<?>> packetContainer) {
        if (isCancelled(packetContainer)) {
            return false;
        }

        protocolSwapHandler.swapProtocolIfNeeded(packetContainer);
        packetSendMarker.set(packetContainer);

        return true;
    }

    public <WP extends WrappedPacket<WP>> boolean write(PacketContainer<WP> packetContainer) {
        protocolSwapHandler.swapProtocolIfNeeded(packetContainer);
        Bukkit.getLogger().info("Write " + packetContainer.getType());

        return !isCancelled(packetContainer);
    }
}
