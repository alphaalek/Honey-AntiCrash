package me.alek.honey.netty.packet;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.ObjectPacketStructure;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.AutoReleasable;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.Optional;

public final class InternalPacketContainer<WP extends WrappedPacket<WP>> implements PacketContainer<WP> {

    private PacketType type;
    private boolean cancelled;
    private boolean dumped;
    private PipelineState pipelineState;
    private InternalObjectPacketStructure structure;
    private ObjectWrappedPacket<WP> objectWrapped;
    private ByteWrappedPacket<WP> initialByteWrapped;
    private ByteWrappedPacket<WP> finalByteWrapped;
    private final Callback postAction;
    private final HoneyPlayer player;
    private final long timeCreated = System.currentTimeMillis();
    private final PacketLifecycleTimings eventLoopStateTracker = new PacketLifecycleTimings();
    private final Callback callbackAction = new Callback();

    public InternalPacketContainer(Object handle, HoneyPlayer player, PacketType type, Callback postAction) {
        this(PipelineState.OBJECT, null, player, type, postAction);
        callPostMinecraftDecode(handle, type);
    }

    public InternalPacketContainer(PipelineState createdState, PacketDataSerializer serializer, HoneyPlayer player, PacketType type, Callback postAction) {
        this.player = player;
        this.type = type;
        Callback wrapped = HoneyPlugin.get().getPacketLibrary().getPacketProcessor().getPostAction(type);
        if (wrapped == null) {
            this.postAction = postAction;
        } else {
            if (postAction != null && postAction != wrapped) {
                postAction.getActions().forEach(wrapped::addAction);
            }
            this.postAction = wrapped;
        }

        this.pipelineState = createdState;
        if (serializer != null && createdState != PipelineState.OBJECT) {
            ByteWrappedPacket<WP> byteWrappedPacket = WrappedPacketFactoryCollection.initWrappedPacket(type, createdState, serializer, this);
            if (createdState == PipelineState.INITIAL_BYTE) {
                this.initialByteWrapped = byteWrappedPacket;
            } else if (createdState == PipelineState.FINAL_BYTE) {
                this.finalByteWrapped = byteWrappedPacket;
            }
        }
    }

    @Override
    public void callPostMinecraftDecode(Object handle, PacketType type) {
        this.type = type;
        this.pipelineState = PipelineState.OBJECT;

        if (PacketTypes.isUnknown(type)
                || !type.getNmsClass().equals(handle.getClass())) {
            throw new IllegalStateException("Unknown type " + handle + " " + type + " " + Optional.ofNullable(type).map(PacketType::getNmsClass).orElse(null));
        }

        ReflectStructure<Object, ?> structure = ReflectStructure.Provider.acquireStructure(type);
        this.structure = new InternalObjectPacketStructure(handle, structure);

        this.objectWrapped = WrappedPacketFactoryCollection.initWrappedPacket(type, handle, this);
    }

    @Override
    public void callPreMinecraftDecode(PacketDataSerializer serializer) {
        if (this.pipelineState == PipelineState.INITIAL_BYTE) {
            this.pipelineState = PipelineState.FINAL_BYTE;
            this.finalByteWrapped = WrappedPacketFactoryCollection.initWrappedPacket(type, this.pipelineState, serializer, this);
        }
    }

    @Override
    public NetProtocol getProtocolByBytePacketState() {
        if (this.pipelineState == PipelineState.OBJECT) {
            throw new IllegalStateException("Object state"); // illegal state to call this
        } else if (this.pipelineState == PipelineState.INITIAL_BYTE && this.player.clientInfo != null) {
            return this.player.clientInfo.getProtocol(); // use the client version to check for different protocols
        } else {
            return NetProtocol.getProtocol(); // if modified by ViaVersion, using either client or server should read same packet data
        }
    }

    @Override
    public ObjectPacketStructure getStructure() {
        return this.structure;
    }

    @Override
    public PacketLifecycleTimings getEventloopStateTimings() {
        return this.eventLoopStateTracker;
    }

    @Override
    public long getTimeCreated() {
        return this.timeCreated;
    }

    @Override
    public void addServerCallbackAction(Callback.Action action) {
        this.callbackAction.addAction(action);
    }

    @Override
    public void invokeServerCallback() {
        this.callbackAction.call(this);
    }

    @Override
    public Channel getChannel() {
        return this.player.channel;
    }

    @Override
    public PacketType getType() {
        return this.type;
    }

    @Override
    public PacketState getState() {
        if (this.type == null) {
            return PacketState.UNKNOWN;
        }

        return this.type.getState();
    }

    @Override
    public PipelineState getPipelineState() {
        return this.pipelineState;
    }

    @Override
    public PacketState getProtocolState() {
        return this.player.packetHandler.getProtocolState(type.getBound());
    }

    @Override
    public boolean isDumped() {
        return this.dumped;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void cancel() {
        if (!this.cancelled) {
            HoneyPlugin.get().getNotificationService().showPacketFilterActionBar(player, true, true);
        }

        this.cancelled = true;
    }

    @Override
    public void dump() {
        this.dumped = true;
    }

    @Override
    public void releaseIfAutoReleasable() {
        if (this.initialByteWrapped instanceof AutoReleasable) {
            ((AutoReleasable)this.initialByteWrapped).release();
        }
        if (this.finalByteWrapped instanceof AutoReleasable) {
            ((AutoReleasable)this.finalByteWrapped).release();
        }
        if (this.objectWrapped instanceof AutoReleasable) {
            ((AutoReleasable)this.objectWrapped).release();
        }
    }

    @Override
    public Callback getPostAction() {
        return this.postAction;
    }

    @Override
    public ObjectWrappedPacket<WP> getObjectWrapped() {
        return this.objectWrapped;
    }

    @Override
    public ByteWrappedPacket<WP> getInitialByteWrapped() {
        return this.initialByteWrapped;
    }

    @Override
    public ByteWrappedPacket<WP> getFinalByteWrapped() {
        return this.finalByteWrapped;
    }

    @Override
    public WP getAnyWrapped() {
        if (this.initialByteWrapped != null && !this.initialByteWrapped.getClass().equals(ByteWrappedPacket.class)) {
            return this.initialByteWrapped.get();
        } else if (this.finalByteWrapped != null && !this.finalByteWrapped.getClass().equals(ByteWrappedPacket.class)) {
            return this.finalByteWrapped.get();
        } else if (this.objectWrapped != null && !this.objectWrapped.getClass().equals(ObjectWrappedPacket.class)) {
            return this.objectWrapped.get();
        } else {
            // none of the wrapped packets are specific, just return any abstract instance
            if (this.initialByteWrapped != null) {
                return this.initialByteWrapped.get();
            } else if (this.finalByteWrapped != null) {
                return this.finalByteWrapped.get();
            } else if (this.objectWrapped != null) {
                return this.objectWrapped.get();
            } else {
                return null;
            }
        }
    }

    @Override
    public HoneyPlayer getPlayer() {
        return this.player;
    }

    @Override
    public String getPlayerName() {
        // if this is called before LOGIN_START post action, the player name is not yet updated
        if (this.type == PacketTypes.Login.Client.LOGIN_START) {
            return ((WrappedLoginInLoginStart)getAnyWrapped()).getName();
        }

        return this.player.packetHandler.getPlayerName();
    }

    @Override
    public String toString() {
        return "Packet:{Type:" + Optional.ofNullable(this.type).orElse(PacketTypes.UNKNOWN) + ",Player:" + getPlayerName() + "}";
    }
}
