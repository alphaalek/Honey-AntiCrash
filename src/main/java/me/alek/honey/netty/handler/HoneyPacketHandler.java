package me.alek.honey.netty.handler;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.CodecFactory;
import me.alek.honey.netty.codec.EmulatorCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.WrappedEnumProtocol;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.util.LinkedList;
import java.util.Queue;


public class HoneyPacketHandler {

    @SuppressWarnings("unchecked")
    public static final ThreadLocal<Queue<Runnable>>[] EVENTLOOP_PROCESSES = new ThreadLocal[]{new ThreadLocal<>(), new ThreadLocal<>()};

    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = new ThreadLocal<>();
    private final ProtocolSwapHandler protocolSwapHandler = new ProtocolSwapHandler();
    private final Channel channel;
    private final HoneyPlayer player;
    private PacketState clientboundProtocolState = PacketState.HANDSHAKE;
    private PacketState serverboundProtocolState = PacketState.HANDSHAKE;
    private EmulatorCodec<Object, PacketDataSerializer> clientboundProtocol;
    private EmulatorCodec<Object, PacketDataSerializer> serverboundProtocol;
    private String playerName = "Unknown";

    private boolean resolvingClientbound;
    private boolean resolvingServerbound;

    public HoneyPacketHandler(Channel channel) {
        this.channel = channel;
        this.player = new HoneyPlayer(channel, this);
    }

    public Channel getChannel() {
        return channel;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public void setPlayerName(String playerName) {
        player.name = playerName;
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public PacketState getProtocolState(PacketBound bound) {
        return bound.equals(PacketBound.CLIENT) ? clientboundProtocolState : serverboundProtocolState;
    }

    public EmulatorCodec<Object, PacketDataSerializer> getProtocolCodec(PacketBound bound) {
        return bound.equals(PacketBound.CLIENT) ? clientboundProtocol : serverboundProtocol;
    }

    public PacketType getPacketType(PacketBound bound, int id) {
        return getProtocolState(bound).getTypes()[bound.ordinal()].get(id);
    }

    public void resolve(PacketBound bound, boolean state) {
        if (bound.equals(PacketBound.CLIENT)) {
            resolvingClientbound = state;
        } else if (bound.equals(PacketBound.SERVER)) {
            resolvingServerbound = state;
        }
    }

    public boolean isResolving(PacketBound bound) {
        return bound.equals(PacketBound.CLIENT) ? resolvingClientbound : resolvingServerbound;
    }

    public void updateProtocol(PacketBound bound) {
        if (bound == null) {
            updateProtocol(PacketBound.SERVER);
            updateProtocol(PacketBound.CLIENT);
            return;
        }

        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_6)
                && channel.pipeline().get("pre-resolved-packet-handler") == null
                && !ConfigurationOptions.Decoder.USE_DEFAULT) {
            Attribute<Object> protocolAttribute = channel.attr(WrappedEnumProtocol.getProtocolAttributeKey(channel, bound));
            protocolAttribute.set(WrappedEnumProtocol.getEnumProtocolDelegate(bound, getProtocolState(bound)).getHandle());
        }
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

        updateProtocol(bound);
    }

    public static ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> getPacketSendMarker() {
        return packetSendMarker;
    }

    private boolean isCancelled(PacketContainer<? extends WrappedPacket<?>> packetContainer) {
        if (packetContainer.isCancelled()) {
            if (ConfigurationOptions.PacketLogger.DEBUG_PACKETS_CANCELLED) {
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + getProtocolState(packetContainer.getType().getBound()) + ", " + packetContainer.getType().getBound() + ", " + packetContainer.getPlayerName());
            }

            return true;
        }
        return false;
    }

    public boolean isPacket(Object raw) {
        return NMS.getPacketClass().isInstance(raw);
    }

    public boolean handleNonPacket(Object raw) {
        if (raw.toString().contains("UnconfiguredPipelineHandler")
                && !HoneyPlugin.get().getPacketLibrary().getPipelineInjector().isConfigurationActive(channel.pipeline())) {
            return false;
        }

        return true;
    }

    public synchronized boolean read(PacketContainer<? extends WrappedPacket<?>> packetContainer) {
        Bukkit.getLogger().info(packetContainer.getType() + "");

        if (isCancelled(packetContainer)) {
            return false;
        }

        protocolSwapHandler.swapProtocolIfNeeded(packetContainer);
        packetSendMarker.set(packetContainer);

        return true;
    }

    public <WP extends WrappedPacket<WP>> boolean write(PacketContainer<WP> packetContainer) {
        Bukkit.getLogger().info(packetContainer.getType() + "");
        protocolSwapHandler.swapProtocolIfNeeded(packetContainer);
        return !isCancelled(packetContainer);
    }

    public void handleEventloopProcesses(PacketBound bound) {
        Queue<Runnable> process = EVENTLOOP_PROCESSES[bound.ordinal()].get();
        if (process == null) {
            EVENTLOOP_PROCESSES[bound.ordinal()].set(new LinkedList<>());
        }
        if (process != null && !process.isEmpty()) {
            Runnable ac;
            while ((ac = process.poll()) != null) {
                ac.run();
            }
        }
    }
}
