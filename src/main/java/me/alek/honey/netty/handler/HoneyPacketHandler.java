package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.via.ViaProtocol;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.CodecFactory;
import me.alek.honey.netty.codec.EmulatorCodec;
import me.alek.honey.netty.codec.LegacyProtocol;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.NMS;

import java.util.LinkedList;
import java.util.Queue;


public class HoneyPacketHandler {

    public static final String UNKNOWN = "Unknown";
    @SuppressWarnings("unchecked")
    public static final ThreadLocal<Queue<Runnable>>[] DELAYED_TASKS = new ThreadLocal[]{new ThreadLocal<>(), new ThreadLocal<>()};

    public static final ThreadLocal<PacketContainer<?>> MARK = new ThreadLocal<>();
    private final ProtocolSwapHandler protocolSwapHandler = new ProtocolSwapHandler();
    private final Channel channel;
    private final HoneyPlayer player;
    private ViaProtocol viaProtocol;
    private PacketState clientboundProtocolState = PacketState.HANDSHAKE;
    private PacketState serverboundProtocolState = PacketState.HANDSHAKE;
    private EmulatorCodec<Object, PacketDataSerializer> clientboundProtocol;
    private EmulatorCodec<Object, PacketDataSerializer> serverboundProtocol;
    private String playerName = UNKNOWN;

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
        Attribute<Object> protocolAttribute = channel.attr(LegacyProtocol.getProtocolAttributeKey(channel, bound));
        protocolAttribute.set(LegacyProtocol.getEnumProtocolDelegate(bound, getProtocolState(bound)).getHandle());
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

    public PacketType getPacketType(ByteBuf buf, boolean resetToStartIndex, PacketBound bound, boolean useVia) {
        buf.markReaderIndex();
        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        int id = serializer.readVarInt();
        if (resetToStartIndex) {
            buf.resetReaderIndex();
        }

        PacketState state = getProtocolState(bound);

        // check if the client protocol version is not the same as the server protocol version
        // if it is not the same, then we will acquire the packet type with ViaVersion
        if (useVia
                && ViaProtocol.ViaValidator.VALIDATOR.find().get()
                && state != PacketState.HANDSHAKE
                && !this.player.clientInfo.getProtocol().equals(NetProtocol.getProtocol())) {

            if (this.viaProtocol == null) {
                this.viaProtocol = ViaProtocol.getViaProtocolFrom(this.player.clientInfo.getProtocol());
            }
            return this.viaProtocol.getTypes(bound, state).get(id);
        }

        return getPacketType(PacketBound.SERVER, id);
    }

    public boolean isCancelled(PacketContainer<?> packet) {
        if (packet.isCancelled()) {
            if (ConfigurationOptions.PacketLogger.DEBUG_PACKETS_CANCELLED) {
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packet.getType() + ", " + getProtocolState(packet.getType().getBound()) + ", " + packet.getType().getBound() + ", " + packet.getPlayerName());
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

    public synchronized boolean read(PacketContainer<?> packet) {
        if (isCancelled(packet)) {
            return false;
        }

        protocolSwapHandler.swapProtocolIfNeeded(packet);
        MARK.set(packet);

        return true;
    }

    public boolean write(PacketContainer<?> packet) {
        protocolSwapHandler.swapProtocolIfNeeded(packet);
        return !isCancelled(packet);
    }

    public void executeDelayedTasks(PacketBound bound) {
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
