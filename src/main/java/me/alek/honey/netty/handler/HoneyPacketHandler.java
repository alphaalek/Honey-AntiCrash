package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.via.ViaHandlers;
import me.alek.honey.compat.via.ViaProtocol;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MinecraftReflection;

import java.util.*;


public class HoneyPacketHandler {

    public static final String UNKNOWN = "Unknown";
    @SuppressWarnings("unchecked")
    public static final ThreadLocal<Queue<Runnable>>[] DELAYED_TASKS = new ThreadLocal[]{new ThreadLocal<>(), new ThreadLocal<>()};
    public static final Map<String, HoneyPacketHandler> AWAITING_PROFILES = new HashMap<>();

    public PacketContainer<?> filterMark;
    public PacketContainer<?> finalMark;
    private final ProtocolSwapHandler protocolSwapHandler = new ProtocolSwapHandler();
    private final Channel channel;
    private final HoneyPlayer player;
    private boolean hasCompression;
    private boolean dependentOnVia;
    private boolean relocatedViaInterceptor;
    private ViaProtocol viaProtocol;
    private PacketState clientboundProtocolState = PacketState.HANDSHAKE;
    private PacketState serverboundProtocolState = PacketState.HANDSHAKE;
    private PacketState simulatedClientboundProtocolState = PacketState.HANDSHAKE;
    private PacketState simulatedServerboundProtocolState = PacketState.HANDSHAKE;

    public HoneyPacketHandler(Channel channel) {
        this.channel = channel;
        this.player = new HoneyPlayer(channel, this);
    }

    public Channel getChannel() {
        return this.channel;
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

    public PacketType getPacketType(PacketBound bound, int id) {
        return getPacketType(bound, getProtocolState(bound), id);
    }

    public PacketType getPacketType(PacketBound bound, PacketState state, int id) {
        return state.getTypes()[bound.ordinal()].get(id);
    }

    public int getPacketId(PacketType type) {
        return type.getState().getTypes()[type.getBound().ordinal()].indexOf(type);
    }

    public void setProtocolState(PacketBound bound, PacketState state) {
        if (bound.equals(PacketBound.SERVER)) {
            this.serverboundProtocolState = state;
        }
        if (bound.equals(PacketBound.CLIENT)) {
            this.clientboundProtocolState = state;
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

    public synchronized boolean readPacketAtFinalByte(PacketContainer<?> packet) {
        executeDelayedTasks(PacketBound.SERVER);

        if (isCancelled(packet)) {
            return false;
        }

        this.finalMark = packet;

        // handle registered post actions
        Callback postAction = packet.getPostAction();
        if (postAction != null) {
            postAction.call(packet);
        }

        return true;
    }

    public boolean write(PacketContainer<?> packet) {
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
