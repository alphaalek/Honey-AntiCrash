package me.alek.honey.netty.packet;

import io.netty.channel.Channel;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.*;
import me.alek.honey.netty.packetwrappers.impl.login.WrappedLoginInLoginStart;
import me.alek.honey.netty.structure.ObjectPacketStructure;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.validation.ValidationPipeline;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.impl.NettyDecodeValidations;

import java.util.Optional;

public final class InternalPacketContainer<WP extends SourcedWrappedPacket<WP>> implements PacketContainer<WP> {

    private PacketType type;
    private boolean cancelled;
    private boolean dumped;
    private PipelineState pipelineState;
    private InternalObjectPacketStructure structure;
    private ObjectWrappedPacket<WP> objectWrapped;
    private ByteInWrappedPacket<WP> initialByteWrapped;
    private ByteInWrappedPacket<WP> finalByteWrapped;
    private boolean callingLateInitialByteWrapped;
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

        Callback registeredPostAction = HoneyPlugin.get().getPacketLibrary().getPacketProcessor().getPostAction(type);
        if (registeredPostAction != null && !registeredPostAction.isEmpty()) {
            this.postAction = Optional.ofNullable(postAction).orElseGet(Callback::new);
            this.postAction.getActions().addAll(registeredPostAction.getActions());
        } else {
            this.postAction = postAction;
        }

        this.pipelineState = createdState;
        if (serializer != null && createdState != PipelineState.OBJECT && !PacketTypes.isUnknown(type)) {
            ByteInWrappedPacket<WP> byteWrappedPacket = WrappedPacketFactoryCollection.initWrappedPacket(type, createdState, serializer, this);

            boolean validate = this.initialByteWrapped == null && this.finalByteWrapped == null;
            if (createdState == PipelineState.INITIAL_BYTE) {
                this.initialByteWrapped = byteWrappedPacket;
            } else if (createdState == PipelineState.FINAL_BYTE) {
                this.finalByteWrapped = byteWrappedPacket;
            }
            if (validate) {
                validateBytePacket(byteWrappedPacket);
            }
        }
    }

    private void validateBytePacket(ByteInWrappedPacket<WP> packet) {
        if (this.initialByteWrapped == null && this.finalByteWrapped == null) {
            ValidationPipelineContext context = new ValidationPipelineContext(true);
            ValidationPipeline.applyCallbackDevelopmentData(this, context);
            NettyDecodeValidations.BYTE_FILTER_PIPELINE.doStream(
                    this.player,
                    context,
                    (validation, __) -> validation.validate(this.player, packet, context),
                    this
            );
            if (this.cancelled) {
                throw new DecoderException("Failed byte filter validation");
            }
        }

    }

    @Override
    public void handleRewritten(PacketType type, PipelineState state) {
        if (!this.type.equals(type)) {
            // rewritten new type by ViaVersion
            this.initialByteWrapped = null;
            this.finalByteWrapped = null;
        }
        this.type = type;
        this.pipelineState = state;
    }

    @Override
    public void callPostMinecraftDecode(Object handle, PacketType type) {
        handleRewritten(type, PipelineState.OBJECT);

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
            if (this.finalByteWrapped == null) {
                this.finalByteWrapped = WrappedPacketFactoryCollection.initWrappedPacket(type, this.pipelineState, serializer, this);
            }
            HoneyPlugin.get().getPacketLibrary().getPacketProcessor().callFinalBytePacketProcessListeners(this.player, this);
        }
    }

    @Override
    public void callLateInitialByteListeners(PacketDataSerializer serializer) {
        if (WrappedPacketFactoryCollection.hasByteWrappedPacket(this.type, PacketContainer.PipelineState.INITIAL_BYTE)) {
            this.callingLateInitialByteWrapped = true;
            if (this.initialByteWrapped == null) {
                this.initialByteWrapped = WrappedPacketFactoryCollection.initWrappedPacket(this.type, PipelineState.INITIAL_BYTE, serializer, this);
            }
            HoneyPlugin.get().getPacketLibrary().getPacketProcessor().callInitialBytePacketProcessListeners(this.player, this);
            this.callingLateInitialByteWrapped = false;
        }
    }

    @Override
    public NetProtocol getProtocol() {
        if (!this.callingLateInitialByteWrapped && this.pipelineState == PipelineState.INITIAL_BYTE && this.player.clientInfo != null) {
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
            ((AutoReleasable) this.initialByteWrapped).release();
        }
        if (this.finalByteWrapped instanceof AutoReleasable) {
            ((AutoReleasable) this.finalByteWrapped).release();
        }
        if (this.objectWrapped instanceof AutoReleasable) {
            ((AutoReleasable) this.objectWrapped).release();
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
    public ByteInWrappedPacket<WP> getInitialByteWrapped() {
        return this.initialByteWrapped;
    }

    @Override
    public ByteInWrappedPacket<WP> getFinalByteWrapped() {
        return this.finalByteWrapped;
    }

    @Override
    public WP getAnyWrapped() {
        if (this.objectWrapped != null && !this.objectWrapped.getClass().equals(ObjectWrappedPacket.class)) {
            return this.objectWrapped.get();
        } else if (this.finalByteWrapped != null && !this.finalByteWrapped.getClass().equals(ByteInWrappedPacket.class)) {
            return this.finalByteWrapped.get();
        } else if (this.initialByteWrapped != null && !this.initialByteWrapped.getClass().equals(ByteInWrappedPacket.class)) {
            return this.initialByteWrapped.get();
        } else {
            // none of the wrapped packets are specific, just return any abstract instance

            // the abstract object wrapped is prioritized first because packet logging needs an object instance to read properties
            if (this.objectWrapped != null) {
                return this.objectWrapped.get();
            } else if (this.finalByteWrapped != null) {
                return this.finalByteWrapped.get();
            } else if (this.initialByteWrapped != null) {
                return this.initialByteWrapped.get();
            } else {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public WP getCurrentWrapped() {
        if (this.pipelineState.equals(PipelineState.OBJECT)) {
            return this.objectWrapped.get();
        } else if (this.pipelineState.equals(PipelineState.FINAL_BYTE)) {
            return this.finalByteWrapped.get();
        } else if (this.pipelineState.equals(PipelineState.INITIAL_BYTE)) {
            return this.initialByteWrapped.get();
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public HoneyPlayer getPlayer() {
        return this.player;
    }

    @Override
    public String getPlayerName() {
        // if this is called before HELLO post action, the player name is not yet updated
        if (this.type == PacketTypes.Login.Client.HELLO) {
            return ((WrappedLoginInLoginStart) getAnyWrapped()).getUsername();
        }

        return this.player.packetHandler.getPlayerName();
    }

    @Override
    public String toString() {
        return "Packet:{Type:" + Optional.ofNullable(this.type).orElse(PacketType.UNKNOWN) + ",Player:" + getPlayerName() + "}";
    }
}
