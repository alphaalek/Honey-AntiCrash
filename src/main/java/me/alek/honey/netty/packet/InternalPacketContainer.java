package me.alek.honey.netty.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.codec.HoneyPacketCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.*;
import me.alek.honey.netty.structure.ObjectPacketStructure;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.validation.ValidationPipeline;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.impl.NettyDecodeValidations;

import java.util.Optional;

public final class InternalPacketContainer<WP extends SourcedWrappedPacket<WP>> extends PacketContainerLifecycle<WP> implements PacketContainer<WP> {

    private final PipelineState createdPipelineState;
    private PipelineState pipelineState;
    private PacketType type;
    private InternalObjectPacketStructure structure;
    private ObjectWrappedPacket<WP> objectWrapped;
    private ByteInWrappedPacket<WP> initialByteWrapped;
    private ByteInWrappedPacket<WP> finalByteWrapped;
    private boolean callingLateInitialByteWrapped;
    private final Callback postAction;
    private final HoneyPlayer player;
    private final long timeCreated = System.currentTimeMillis();
    private final PacketLifecycleTimings eventLoopStateTracker = new PacketLifecycleTimings();

    public InternalPacketContainer(Object handle, HoneyPlayer player, PacketType type, Callback postAction) {
        this(PipelineState.OBJECT, null, player, type, postAction);
        callPostMinecraftDecode(handle, type);
    }

    public InternalPacketContainer(PipelineState createdState, PacketDataSerializer serializer, HoneyPlayer player, PacketType type, Callback postAction) {
        this.player = player;
        this.type = type;

        this.postAction = new Callback();
        if (postAction != null) {
            this.postAction.getActions().addAll(postAction.getActions());
        }
        applyPostActions();

        this.createdPipelineState = this.pipelineState = createdState;
        if (serializer != null && !createdState.equals(PipelineState.OBJECT) && !PacketTypes.isUnknown(type)
                && (this.initialByteWrapped == null || !this.initialByteWrapped.isAbstract())
                && (this.finalByteWrapped == null || !this.finalByteWrapped.isAbstract())
        ) {
            if (this.initialByteWrapped == null && this.finalByteWrapped == null) {
                // ignore spam packets
                if (type != PacketTypes.Play.Client.PONG
                        && type != PacketTypes.Play.Client.TRANSACTION
                        && type != PacketTypes.Play.Client.TICK_END
                        && type != PacketTypes.Play.Client.PLAYER_MOVEMENT) {
                    // validate this buffer
                    ByteInWrappedPacket<WP> pre = new ByteInWrappedPacketImpl<>(serializer, this);
                    if (createdState.equals(PipelineState.INITIAL_BYTE)) {
                        this.initialByteWrapped = pre;
                    } else if (createdState.equals(PipelineState.FINAL_BYTE)) {
                        this.finalByteWrapped = pre;
                    }
                    validateBytePacket(pre);
                }
            }
            // construct the wrapped packet buffer
            ByteInWrappedPacket<WP> byteWrappedPacket = WrappedPacketFactoryCollection.initWrappedPacket(type, createdState, serializer, this);
            if (createdState.equals(PipelineState.INITIAL_BYTE)) {
                this.initialByteWrapped = byteWrappedPacket;
            } else if (createdState.equals(PipelineState.FINAL_BYTE)) {
                this.finalByteWrapped = byteWrappedPacket;
            }

            if (byteWrappedPacket.isAbstract()) {
                // check if any bytes has not been decoded
                // if any is left, the protocol has not been followed properly
                HoneyPacketCodec.checkReadable(getProtocolCreated(), type, Optional.ofNullable(this.objectWrapped).map(ObjectWrappedPacket::getHandle).orElse(null), serializer);
                serializer.readerIndex(0);
            }
        }
    }

    private void validateBytePacket(ByteInWrappedPacket<WP> packet) {
        ValidationPipelineContext context = new ValidationPipelineContext(true);
        ValidationPipeline.applyCallbackDevelopmentData(this, context);

        NettyDecodeValidations.BYTE_FILTER_PIPELINE.doStream(
                this.player,
                context,
                (validation, __) -> validation.validate(this.player, packet, context),
                this);
        if (this.cancelled) {
            throw new DecoderException("Failed byte filter validation");
        }
    }

    @Override
    public PacketDataSerializer rewrite(PacketDataSerializer serializer) {
        if (this.pipelineState.equals(PipelineState.OBJECT)) {
            throw new IllegalStateException("Can't rewrite object");
        }

        WP packet = getCurrentWrapped();
        // check if this packet should be rewritten
        if (!packet.getState().equals(PipelineState.OBJECT) && packet.isAbstract() && packet instanceof PacketRewriter) {
            PacketRewriter rewriter = (PacketRewriter) packet;
            if (rewriter.shouldRewrite()) {
                // rewrite the buffer
                serializer = new PacketDataSerializer();
                serializer.writeVarInt(this.type.getMinecraftID().get());
                rewriter.rewrite(serializer);
                return serializer;
            }
        }

        return serializer.retain();
    }

    private void applyPostActions() {
        Callback registeredPostAction = HoneyPlugin.get().getPacketLibrary().getPacketProcessor().getPostAction(this.type);
        if (registeredPostAction != null) {
            this.postAction.getActions().addAll(registeredPostAction.getActions());
        }
    }

    @Override
    public void handleUnknownRewritten(PacketType type, PipelineState state) {
        if (!this.type.equals(type)) {
            // rewritten new type by ViaVersion
            this.type = type;
            this.handled = false;
            this.initialByteWrapped = null;

            this.postAction.clear();
            applyPostActions();
        }
        this.pipelineState = state;
    }

    @Override
    public void callPostMinecraftDecode(Object handle, PacketType type) {
        handleUnknownRewritten(type, PipelineState.OBJECT);

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
                serializer.markReaderIndex();
                this.finalByteWrapped = WrappedPacketFactoryCollection.initWrappedPacket(type, this.pipelineState, serializer, this);

                if (this.finalByteWrapped.isAbstract()) {
                    // check if any bytes has not been decoded
                    // if any is left, the protocol has not been followed properly
                    HoneyPacketCodec.checkReadable(getProtocolCreated(), this.type, Optional.ofNullable(this.objectWrapped).map(ObjectWrappedPacket::getHandle).orElse(null), serializer);
                    serializer.resetReaderIndex();
                }
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
        if (!this.callingLateInitialByteWrapped && this.pipelineState == PipelineState.INITIAL_BYTE && !getState().equals(PacketState.HANDSHAKE)) {
            // use the client-side protocol
            return getProtocolCreated();
        } else {
            // if modified by ViaVersion (or not) at this state, using either client or server should read same packet data
            return NetProtocol.getProtocol();
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
    public Channel getChannel() {
        return this.player.channel;
    }

    @Override
    public PacketType getType() {
        return this.type;
    }

    @Override
    public PacketState getState() {
        return Optional.ofNullable(this.type).map(PacketType::getState).orElse(PacketState.UNKNOWN);
    }

    @Override
    public PipelineState getPipelineState() {
        return this.pipelineState;
    }

    @Override
    public PipelineState getCreatedPipelineState() {
        return this.createdPipelineState;
    }

    @Override
    public PacketState getProtocolState() {
        return getPacketHandler().getProtocolState(this.type.getBound());
    }

    @Override
    public void cancel() {
        if (!this.cancelled) {
            HoneyPlugin.get().getNotificationService().showPacketFilterActionBar(this.player, true, true);
        }

        this.cancelled = true;
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
    public ByteInWrappedPacket<WP> getAnyByteWrapped() {
        if (this.initialByteWrapped != null && this.initialByteWrapped.isAbstract()) {
            return this.initialByteWrapped;
        } else if (this.finalByteWrapped != null && this.finalByteWrapped.isAbstract()) {
            return this.finalByteWrapped;
        } else {
            if (this.initialByteWrapped != null) {
                return this.initialByteWrapped;
            } else if (this.finalByteWrapped != null) {
                return this.finalByteWrapped;
            } else {
                return null;
            }
        }
    }

    @Override
    public WP getAnyWrapped() {
        if (this.objectWrapped != null && this.objectWrapped.isAbstract()) {
            return this.objectWrapped.get();
        } else if (this.finalByteWrapped != null && this.finalByteWrapped.isAbstract()) {
            return this.finalByteWrapped.get();
        } else if (this.initialByteWrapped != null && this.initialByteWrapped.isAbstract()) {
            return this.initialByteWrapped.get();
        } else {
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
        } else if ((this.pipelineState.equals(PipelineState.INITIAL_BYTE) || this.callingLateInitialByteWrapped)) {
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
    public String toString() {
        return "Packet:{Type:" + Optional.ofNullable(this.type).orElse(PacketType.UNKNOWN) + ",Player:" + getPlayerName() + "}";
    }
}
