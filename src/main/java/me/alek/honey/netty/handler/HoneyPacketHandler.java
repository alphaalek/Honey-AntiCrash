package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.via.ViaProtocol;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.CodecFactory;
import me.alek.honey.netty.codec.EmulatorCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.WrappedEnumProtocol;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

import java.util.LinkedList;
import java.util.Queue;


public class HoneyPacketHandler {

    @SuppressWarnings("unchecked")
    public static final ThreadLocal<Queue<Runnable>>[] DELAYED_TASKS = new ThreadLocal[]{new ThreadLocal<>(), new ThreadLocal<>()};

    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = new ThreadLocal<>();
    private final ProtocolSwapHandler protocolSwapHandler = new ProtocolSwapHandler();
    private final Channel channel;
    private final HoneyPlayer player;
    private ViaProtocol viaProtocol;
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

    public void updateProtocol(PacketState previous, PacketBound bound) {
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_6)) {
            if (bound.equals(PacketBound.SERVER)
                    && ConfigurationOptions.Decoder.USE_DEFAULT) {
                return;
            }

            if (previous.equals(PacketState.HANDSHAKE)
                    || bound.equals(PacketBound.SERVER)) {
                updateProtocolAttributeKey(bound);
            } else {
                DELAYED_TASKS[bound.ordinal()].get().add(() -> {
                    updateProtocolAttributeKey(bound);
                });
            }
        }
    }

    private void updateProtocolAttributeKey(PacketBound bound) {
        Attribute<Object> protocolAttribute = channel.attr(WrappedEnumProtocol.getProtocolAttributeKey(channel, bound));
        protocolAttribute.set(WrappedEnumProtocol.getEnumProtocolDelegate(bound, getProtocolState(bound)).getHandle());
    }

    public void setProtocolState(PacketBound bound, PacketState state) {
        if (!PacketBound.CLIENT.equals(bound)) {
            PacketState previous = getProtocolState(PacketBound.SERVER);
            serverboundProtocolState = state;
            serverboundProtocol = CodecFactory.createInboundCodec(state);
            updateProtocol(previous, PacketBound.SERVER);
        }
        if (!PacketBound.SERVER.equals(bound)) {
            PacketState previous = getProtocolState(PacketBound.CLIENT);
            clientboundProtocolState = state;
            clientboundProtocol = CodecFactory.createOutboundCodec(state);
            updateProtocol(previous, PacketBound.CLIENT);
        }
    }

    public PacketType getPacketType(ByteBuf buf, boolean resetToStartIndex) {
        int startIndex = buf.readerIndex();
        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        int id = serializer.readVarInt();
        if (resetToStartIndex) {
            buf.readerIndex(startIndex);
        }

        PacketState state = getProtocolState(PacketBound.SERVER);
        PacketType type = null;

        // check if the client protocol version is not the same as the server protocol version
        // if it is not the same, then we will acquire the packet type with ViaVersion
        if (ViaProtocol.ViaValidator.VALIDATOR.find().get()
                && state != PacketState.HANDSHAKE // client-info (handshake SET_PROTOCOL) has been called
                && !player.clientInfo.getProtocol().equals(NetProtocol.getProtocol())) {

            if (viaProtocol == null) {
                viaProtocol = ViaProtocol.getViaProtocolFrom(player.clientInfo.getProtocol());
            }

            if (viaProtocol.hasState(PacketBound.SERVER, state)) {
                type = viaProtocol.getServerboundMappings(state).get(id);
            }
        }

        if (type == null) {
            // we didn't acquire the type from ViaVersion type-mapping, just get it by the server
            type = getPacketType(PacketBound.SERVER, id);
        }

        return type;
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

    public void runDelayedTasks(PacketBound bound) {
        Queue<Runnable> process = DELAYED_TASKS[bound.ordinal()].get();
        if (process == null) {
            DELAYED_TASKS[bound.ordinal()].set(new LinkedList<>());
        }
        if (process != null && !process.isEmpty()) {
            Runnable ac;
            while ((ac = process.poll()) != null) {
                ac.run();
            }
        }
    }
}
