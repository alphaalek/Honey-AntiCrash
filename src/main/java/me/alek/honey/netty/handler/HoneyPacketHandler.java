package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.EmulatorCodec;
import me.alek.honey.netty.codec.CodecFactory;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.packetwrappers.WrappedPacket;


public class HoneyPacketHandler {

    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = new ThreadLocal<>();
    private final ProtocolSwapHandler protocolSwapHandler = new ProtocolSwapHandler();
    private final Channel channel;
    private volatile HoneyPlayer player;
    private PacketState clientboundProtocolState = PacketState.HANDSHAKE;
    private PacketState serverboundProtocolState = PacketState.HANDSHAKE;
    private EmulatorCodec<Object, PacketDataSerializer> clientboundProtocol;
    private EmulatorCodec<Object, PacketDataSerializer> serverboundProtocol;
    private String playerName;

    public HoneyPacketHandler(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
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

    public EmulatorCodec<Object, PacketDataSerializer> getProtocol(PacketBound bound) {
        return bound == PacketBound.CLIENT ? clientboundProtocol : serverboundProtocol;
    }

    public void updateProtocol(PacketBound bound) {

        /*
        if (channel.pipeline().get("pre-resolved-packet-handler") == null && !ConfigurationOptions.Decoder.USE_DEFAULT) {
            Attribute<Object> protocolAttribute = channel.attr(WrappedEnumProtocol.getProtocolAttributeKey(channel, bound));
            protocolAttribute.set(WrappedEnumProtocol.getProtocol(bound).getHandle());
        }
        */
    }

    public void setProtocolState(PacketBound bound, PacketState state) {
        if (bound == PacketBound.SERVER) {
            serverboundProtocolState = state;
            serverboundProtocol = CodecFactory.createInboundCodec(getProtocolState(PacketBound.SERVER));
        } else if (bound == PacketBound.CLIENT) {
            clientboundProtocolState = state;
            clientboundProtocol = CodecFactory.createOutboundCodec(getProtocolState(PacketBound.CLIENT));
        } else {
            serverboundProtocolState = state;
            clientboundProtocolState = state;
            serverboundProtocol = CodecFactory.createInboundCodec(getProtocolState(PacketBound.SERVER));
            clientboundProtocol = CodecFactory.createOutboundCodec(getProtocolState(PacketBound.CLIENT));
        }
    }

    public static ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> getPacketSendMarker() {
        return packetSendMarker;
    }

    private boolean isCancelled(PacketContainer<? extends WrappedPacket<?>> packetContainer) {
        if (packetContainer.isCancelled()) {
            if (ConfigurationOptions.PacketLogger.DEBUG_PACKETS_CANCELLED && packetContainer.getPlayer() != null) {
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + getProtocolState(packetContainer.getType().getBound()) + ", " + packetContainer.getType().getBound() + ", " + packetContainer.getPlayerName());
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
        return !isCancelled(packetContainer);
    }
}
