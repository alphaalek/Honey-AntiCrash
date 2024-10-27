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
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.Callback;

import java.util.Optional;

public final class InternalPacketContainer<WP extends WrappedPacket<WP>> implements PacketContainer<WP> {

    private PacketType type;
    private boolean cancelled;
    private boolean dumped;
    private State state;
    private InternalObjectPacketStructure structure;
    private ObjectWrappedPacket<WP> objectWrapped;
    private final ByteWrappedPacket<WP> byteWrapped;
    private final Callback postAction;
    private final HoneyPlayer player;
    private final long timeCreated = System.currentTimeMillis();
    private final PacketLifecycleTimings eventLoopStateTracker = new PacketLifecycleTimings();
    private final Callback callbackAction = new Callback();

    public InternalPacketContainer(Object handle, HoneyPlayer player, PacketType type, Callback postAction) {
        this(null, player, type, postAction);
        initPostMinecraftDecode(handle, type);
    }

    public InternalPacketContainer(PacketDataSerializer serializer, HoneyPlayer player, PacketType type, Callback postAction) {
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

        if (serializer != null) {
            this.byteWrapped = WrappedPacketFactoryCollection.initWrappedPacket(type, serializer, this);
            this.state = State.BYTE;
        } else {
            this.byteWrapped = null;
        }
    }

    public void initPostMinecraftDecode(Object handle, PacketType type) {
        this.type = type;
        this.state = State.OBJECT;

        if (PacketTypes.isUnknown(type)
                || !type.getNmsClass().equals(handle.getClass())) {
            throw new IllegalStateException("Unknown type " + handle + " " + type + " " + Optional.ofNullable(type).map(PacketType::getNmsClass).orElse(null));
        }

        ReflectStructure<Object, ?> structure = ReflectStructure.Provider.acquireStructure(type);
        this.structure = new InternalObjectPacketStructure(handle, structure);

        this.objectWrapped = WrappedPacketFactoryCollection.initWrappedPacket(type, handle, this);
    }

    @Override
    public ObjectPacketStructure getStructure() {
        return this.structure;
    }

    @Override
    public String toString() {
        return "Packet:{Type:" + Optional.ofNullable(this.type).orElse(PacketTypes.UNKNOWN) + ",Player:" + getPlayerName() + "}";
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
    public State getProcessState() {
        return this.state;
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
    public Callback getPost() {
        return this.postAction;
    }

    @Override
    public ObjectWrappedPacket<WP> getObjectWrapped() {
        return this.objectWrapped;
    }

    @Override
    public ByteWrappedPacket<WP> getByteWrapped() {
        return this.byteWrapped;
    }

    @Override
    public WP getAnyWrapped() {
        if (this.byteWrapped != null && !this.byteWrapped.getClass().equals(ByteWrappedPacket.class)) {
            return this.byteWrapped.get();
        } else if (this.objectWrapped != null && !this.objectWrapped.getClass().equals(ObjectWrappedPacket.class)) {
            return this.objectWrapped.get();
        } else {
            if (this.byteWrapped != null) {
                return this.byteWrapped.get();
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
        if (this.type == PacketTypes.Login.Client.LOGIN_START && this.objectWrapped != null) {
            return ((WrappedLoginInLoginStart)this.objectWrapped).getName();
        }

        return this.player.packetHandler.getPlayerName();
    }
}
