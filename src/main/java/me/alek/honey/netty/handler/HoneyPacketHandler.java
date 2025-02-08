package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.util.Attribute;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.async.AsyncPacketStream;
import me.alek.honey.async.WorkerPool;
import me.alek.honey.compat.via.ViaHandlers;
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
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MinecraftReflection;
import org.bukkit.Bukkit;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class HoneyPacketHandler {

    public static final String UNKNOWN = "Unknown";
    @SuppressWarnings("unchecked")
    public static final ThreadLocal<Queue<Runnable>>[] DELAYED_TASKS = new ThreadLocal[]{new ThreadLocal<>(), new ThreadLocal<>()};

    public static final ThreadLocal<PacketContainer<?>> MARK = new ThreadLocal<>();
    private final ProtocolSwapHandler protocolSwapHandler = new ProtocolSwapHandler();
    private final Channel channel;
    private final HoneyPlayer player;
    private final AsyncPacketStream asyncPacketStream;
    private boolean hasCompression;
    private boolean dependentOnVia;
    private boolean relocatedViaInterceptor;
    private ViaProtocol viaProtocol;
    private PacketState clientboundProtocolState = PacketState.HANDSHAKE;
    private PacketState serverboundProtocolState = PacketState.HANDSHAKE;
    private PacketState simulatedClientboundProtocolState = PacketState.HANDSHAKE;
    private PacketState simulatedServerboundProtocolState = PacketState.HANDSHAKE;
    private EmulatorCodec<Object, PacketDataSerializer> clientboundProtocol;
    private EmulatorCodec<Object, PacketDataSerializer> serverboundProtocol;

    private boolean configuringClientbound;
    private boolean configuringServerbound;
    private boolean resolvingClientbound;
    private boolean resolvingServerbound;

    public HoneyPacketHandler(Channel channel) {
        this.channel = channel;
        this.player = new HoneyPlayer(channel, this);
        this.asyncPacketStream = new AsyncPacketStream(player, WorkerPool.ASYNC_PACKET_DECODER_POOL);
    }

    public Channel getChannel() {
        return this.channel;
    }

    public AsyncPacketStream getAsyncPacketStream() {
        return this.asyncPacketStream;
    }

    public HoneyPlayer getPlayer() {
        return this.player;
    }

    public void hasCompression() {
        this.hasCompression = true;
    }

    public ProtocolSwapHandler getProtocolSwapHandler() {
        return this.protocolSwapHandler;
    }

    public PacketState getProtocolState(PacketBound bound) {
        return bound.equals(PacketBound.CLIENT) ? this.clientboundProtocolState : this.serverboundProtocolState;
    }

    public PacketState getSimulatedClientboundProtocolState() {
        return this.simulatedClientboundProtocolState;
    }

    public PacketState getSimulatedServerboundProtocolState() {
        return this.simulatedServerboundProtocolState;
    }

    public EmulatorCodec<Object, PacketDataSerializer> getProtocolCodec(PacketBound bound) {
        return bound.equals(PacketBound.CLIENT) ? this.clientboundProtocol : this.serverboundProtocol;
    }

    public PacketType getPacketType(PacketBound bound, int id) {
        return getPacketType(bound, getProtocolState(bound), id);
    }

    public PacketType getPacketType(PacketBound bound, PacketState state, int id) {
        return state.getTypes()[bound.ordinal()].get(id);
    }

    public int getPacketId(PacketType type) {
        return type.getState().getTypes()[type.getBound().ordinal()].indexOf(type);
    }

    public void resolve(PacketBound bound, boolean state) {
        if (bound.equals(PacketBound.CLIENT)) {
            this.resolvingClientbound = state;
        } else if (bound.equals(PacketBound.SERVER)) {
            this.resolvingServerbound = state;
        }
    }

    public boolean isResolving(PacketBound bound) {
        return bound.equals(PacketBound.CLIENT) ? this.resolvingClientbound : this.resolvingServerbound;
    }

    public boolean isConfiguring(PacketBound bound) {
        return bound.equals(PacketBound.CLIENT) ? this.configuringClientbound : this.configuringServerbound;
    }

    public boolean hasResolvedBound(PacketBound bound) {
        return bound.equals(PacketBound.SERVER) && HoneyPlugin.get().getPacketLibrary().getChannelManager().getInStream(this.channel) != null
                || bound.equals(PacketBound.CLIENT) && HoneyPlugin.get().getPacketLibrary().getChannelManager().getOutStream(this.channel) != null;
    }

    public void updateProtocol(PacketBound bound) {
        if (!ConfigurationOptions.Decoder.USE_DEFAULT && hasResolvedBound(bound)) {
            // emulate protocol swap handler if not handled in default codec handlers
            if (NetProtocol.getProtocol().isBetween(NetProtocol.v1_20_2, NetProtocol.v1_20_4)) {
                Attribute<Object> protocolAttribute = this.channel.attr(LegacyProtocol.getProtocolAttributeKey(this.channel, bound));
                protocolAttribute.set(LegacyProtocol.getEnumProtocolDelegate(bound, getProtocolState(bound)).getHandle());
            } else if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                ChannelHandler handler = ModernCodecHandlerFactory.createCodecHandler(bound, getProtocolState(bound));
                this.channel.pipeline().replace(this.channel.pipeline().get(bound.getProtocolHandlerName()), bound.getProtocolHandlerName(), handler);
            }
        }
    }

    public void setProtocolState(PacketBound bound, PacketState state) {
        if (bound.equals(PacketBound.SERVER)) {
            this.serverboundProtocolState = state;
            this.serverboundProtocol = getCodecFor(bound, state);
            updateProtocol(PacketBound.SERVER);
        }

        if (bound.equals(PacketBound.CLIENT)) {
            this.clientboundProtocolState = state;
            this.clientboundProtocol = getCodecFor(bound, state);
            updateProtocol(PacketBound.CLIENT);
        }
    }

    public void setSimulatedClientboundProtocolState(PacketState state) {
        this.simulatedClientboundProtocolState = state;
    }

    public void setSimulatedServerboundProtocolState(PacketState state) {
        this.simulatedServerboundProtocolState = state;
    }

    public void setGlobalClientboundProtocolState(PacketState state) {
        setSimulatedClientboundProtocolState(state);
        setProtocolState(PacketBound.CLIENT, state);
    }

    public void setGlobalServerboundProtocolState(PacketState state) {
        setSimulatedServerboundProtocolState(state);
        setProtocolState(PacketBound.SERVER, state);
    }

    private EmulatorCodec<Object, PacketDataSerializer> getCodecFor(PacketBound bound, PacketState state) {
        if (state.equals(PacketState.CONFIGURATION) && NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_2)) {
            return CodecFactory.NO_OP_CODEC;
        } else {
            if (bound.equals(PacketBound.SERVER)) {
                return CodecFactory.createInboundCodec(state);
            } else {
                return CodecFactory.createOutboundCodec(state);
            }
        }
    }

    public void setConfiguration(PacketBound bound) {
        if (bound.equals(PacketBound.SERVER) && !HoneyPlugin.get().getPacketLibrary().getPipelineInjector().isConfigurationActive(this.channel.pipeline(), bound)) {
            this.configuringServerbound = true;
            this.channel.config().setAutoRead(false);
            this.channel.pipeline().addBefore(MinecraftHandlers.DECODER, MinecraftHandlers.INBOUND_CONFIG, new UnconfiguredPipelineHandler.Inbound());
            this.channel.pipeline().remove(MinecraftHandlers.DECODER);

            if (HoneyPlugin.get().getPacketLibrary().getChannelManager().getInStream(this.channel) != null) {
                this.channel.pipeline().remove(HoneyPlugin.get().getPacketLibrary().getInStreamName());
            }
        }
        if (bound.equals(PacketBound.CLIENT) && !HoneyPlugin.get().getPacketLibrary().getPipelineInjector().isConfigurationActive(this.channel.pipeline(), bound)) {
            this.configuringClientbound = true;
            this.channel.pipeline().addAfter(MinecraftHandlers.ENCODER, MinecraftHandlers.OUTBOUND_CONFIG, new UnconfiguredPipelineHandler.Outbound());
            this.channel.pipeline().remove(MinecraftHandlers.ENCODER);

            if (HoneyPlugin.get().getPacketLibrary().getChannelManager().getOutStream(this.channel) != null) {
                this.channel.pipeline().remove(HoneyPlugin.get().getPacketLibrary().getOutStreamName());
            }
        }

        HoneyPlugin.get().getPacketLibrary().getPipelineInjector().togglePreResolvedBounds(this.channel, this);
    }

    public PacketType getPacketType(ByteBuf buf, boolean resetToStartIndex, PacketState state, PacketBound bound, boolean useVia) {
        buf.markReaderIndex();
        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        int id = serializer.readVarInt();
        if (resetToStartIndex) {
            buf.resetReaderIndex();
        }
        // check if the client protocol version is not the same as the server protocol version
        // if it is not the same, then we will acquire the packet type with ViaVersion
        if (useVia
                && ViaProtocol.ViaValidator.VALIDATOR.find().get()
                && !state.equals(PacketState.HANDSHAKE)
                && !this.player.clientInfo.getProtocol().equals(NetProtocol.getProtocol())) {

            if (this.viaProtocol == null) {
                this.viaProtocol = ViaProtocol.getViaProtocolFrom(this.player.clientInfo.getProtocol());
            }
            return this.viaProtocol.getTypes(bound, state).get(id);
        }

        return getPacketType(PacketBound.SERVER, state, id);
    }

    public PacketType getPacketType(ByteBuf buf, boolean resetToStartIndex, PacketBound bound, boolean useVia) {
        return getPacketType(buf, resetToStartIndex, getProtocolState(bound), bound, useVia);
    }

    public int getPacketId(PacketType type, boolean useVia) {
        if (useVia
                && ViaProtocol.ViaValidator.VALIDATOR.find().get()
                && type.getState() != PacketState.HANDSHAKE
                && !this.player.clientInfo.getProtocol().equals(NetProtocol.getProtocol())) {

            if (this.viaProtocol == null) {
                this.viaProtocol = ViaProtocol.getViaProtocolFrom(this.player.clientInfo.getProtocol());
            }
            return this.viaProtocol.getTypes(type.getBound(), type.getState()).indexOf(type);
        }

        return getPacketId(type);
    }

    public boolean isCancelled(PacketContainer<?> packet) {
        if (packet.isCancelled()) {
            if (ConfigurationOptions.PacketLogger.DEBUG_PACKETS_CANCELLED) {
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: Packet cancelled: " + packet.getType() + ", " + getProtocolState(packet.getType().getBound()) + ", " + packet.getType().getBound() + ", " + packet.getPlayerName());
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean isPacket(Object raw) {
        return MinecraftReflection.getPacketClass().isInstance(raw);
    }

    public synchronized boolean read(PacketContainer<?> packet) {
        if (this.configuringServerbound && !HoneyPlugin.get().getPacketLibrary().getPipelineInjector().isConfigurationActive(this.channel.pipeline(), PacketBound.SERVER)) {
            this.configuringServerbound = false;
        }
        executeDelayedTasks(PacketBound.SERVER);

        if (isCancelled(packet)) {
            return false;
        }

        MARK.set(packet);

        return true;
    }

    public boolean write(PacketContainer<?> packet) {
        if (this.configuringClientbound && !HoneyPlugin.get().getPacketLibrary().getPipelineInjector().isConfigurationActive(this.channel.pipeline(), PacketBound.CLIENT)) {
            this.configuringClientbound = false;
        }
        checkViaCompressionOrder();
        executeDelayedTasks(PacketBound.CLIENT);

        return !isCancelled(packet);
    }

    private void checkViaCompressionOrder() {
        if (this.dependentOnVia && !this.relocatedViaInterceptor && this.hasCompression) {
            List<String> handlers = this.channel.pipeline().names();
            if (handlers.indexOf(ViaClientboundInterceptor.NAME) < handlers.indexOf(MinecraftHandlers.COMPRESS)) {
                this.channel.pipeline().addAfter(MinecraftHandlers.COMPRESS, ViaHandlers.ENCODER, this.channel.pipeline().remove(ViaHandlers.ENCODER));
                this.channel.pipeline().addAfter(MinecraftHandlers.DECOMPRESS, ViaHandlers.DECODER, this.channel.pipeline().remove(ViaHandlers.DECODER));
                this.channel.pipeline().addBefore(ViaHandlers.ENCODER, ViaClientboundInterceptor.NAME, this.channel.pipeline().remove(ViaClientboundInterceptor.NAME));
                this.relocatedViaInterceptor = true;
            }
        }
    }

    public void handleViaClientboundInterceptor() {
        if (ViaProtocol.ViaValidator.VALIDATOR.find().get()
                && !this.player.clientInfo.getProtocol().equals(NetProtocol.getProtocol())
                && this.channel.pipeline().get(ViaHandlers.ENCODER) != null
                && this.channel.pipeline().get(ViaClientboundInterceptor.NAME) == null) {
            this.dependentOnVia = true;
            this.channel.pipeline().addBefore(ViaHandlers.ENCODER, ViaClientboundInterceptor.NAME, new ViaClientboundInterceptor(this));
        }
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
