package me.alek.honey.netty.packet;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.CachedReflectStructureProvider;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.util.Callback;
import me.alek.honey.util.reflection.NMS;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public final class InternalPacketContainer<WP extends WrappedPacket<WP>> implements PacketContainer<WP> {

    private PacketType type;
    private Object raw;
    private WP wrapped;
    private final int byteSize;
    private Callback postAction;
    private final HoneyPlayer player;
    private final Channel channel;
    private ReflectStructure<Object, ?> structure;
    private boolean cancelled;
    private boolean dumped;
    private final long time = System.currentTimeMillis();
    private final PacketLifecycleTimings eventLoopStateTracker = new PacketLifecycleTimings();
    private final Callback callbackAction = new Callback();

    public InternalPacketContainer(Object rawPacket, HoneyPlayer player, Channel channel, Callback postAction, PacketType type) {
        this(player, channel, type, 0);
        postDecode(rawPacket, postAction, type);
    }

    public InternalPacketContainer(HoneyPlayer player, Channel channel, PacketType type, int byteSize) {
        this.player = player;
        this.channel = channel;
        this.type = type;
        this.byteSize = byteSize;
    }

    public void postDecode(Object raw, Callback postAction, PacketType type) {
        this.type = type;
        this.raw = raw;
        this.postAction = postAction;

        if (PacketTypes.isUnknown(type)
                || !type.getNmsClass().equals(raw.getClass())) {
            throw new IllegalStateException("Unknown type " + raw + " " + type + " " + Optional.ofNullable(type).map(PacketType::getNmsClass).orElse(null));
        }

        this.structure = CachedReflectStructureProvider.acquireStructure(type);
        if (this.structure.isStaticFieldsAllowed()) {
            this.structure.withoutStatic();
        }
        this.wrapped = (WP) PacketFactory.createWrappedPacket(type, raw, this);
    }

    @Override
    public int getByteSize() {
        return byteSize;
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
    public long getTime() {
        return this.time;
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
        return this.channel;
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
    public PacketState getProtocolState() {
        return this.player.packetHandler.getProtocolState(type.getBound());
    }

    @Override
    public Object getRaw() {
        return this.raw;
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
        return postAction;
    }

    @Override
    public WP getPacket() {
        return wrapped;
    }

    @Override
    public HoneyPlayer getPlayer() {
        return player;
    }

    @Override
    public String getPlayerName() {
        if (this.type == PacketTypes.Login.Client.LOGIN_START && this.wrapped != null) {
            return ((WrappedLoginInLoginStart)this.wrapped).getName();
        }

        return player.packetHandler.getPlayerName();
    }

    public <T> IStructureModifier<T> getModifier(Class<T> clazz) {
        return getModifier(clazz, null);
    }

    public <T, C> IStructureModifier<C> getModifier(Class<T> clazz, JavaConverter<T, C> converter, Object... params) {
        if (this.structure == null) {
            return null;
        }

        return this.structure.withType(clazz, converter).withTarget(this.raw, params);
    }

    @Override
    public IStructureModifier<Double> getDoubles() {
        return getModifier(double.class);
    }

    @Override
    public IStructureModifier<Long> getLongs() {
        return getModifier(long.class);
    }

    @Override
    public IStructureModifier<Integer> getInts() {
        return getModifier(int.class);
    }

    @Override
    public IStructureModifier<Short> getShorts() {
        return getModifier(short.class);
    }

    @Override
    public IStructureModifier<Float> getFloats() {
        return getModifier(float.class);
    }

    @Override
    public IStructureModifier<Byte> getBytes() {
        return getModifier(byte.class);
    }

    @Override
    public IStructureModifier<Boolean> getBooleans() {
        return getModifier(boolean.class);
    }

    @Override
    public IStructureModifier<String> getStrings() {
        return getModifier(String.class);
    }

    @Override
    public IStructureModifier<Object> getObjectsOf() {
        return getModifier(Object.class);
    }

    @Override
    public IStructureModifier<Object> getAll() {
        if (this.structure == null) {
            return null;
        }

        return (IStructureModifier<Object>) this.structure.withTarget(raw);
    }

    @Override
    public IStructureModifier<UUID> getUUIDS() {
        return getModifier(UUID.class);
    }

    @Override
    public IStructureModifier<String[]> getStringArrays() {
        return getModifier(String[].class);
    }

    @Override
    public IStructureModifier<long[]> getLongArrays() {
        return getModifier(long[].class);
    }

    @Override
    public IStructureModifier<int[]> getIntArrays() {
        return getModifier(int[].class);
    }

    @Override
    public IStructureModifier<short[]> getShortArrays() {
        return getModifier(short[].class);
    }

    @Override
    public IStructureModifier<byte[]> getByteArrays() {
        return getModifier(byte[].class);
    }

    @Override
    public IStructureModifier<ItemStack> getItems() {
        return getModifier(NMS.getItemStackClass(), Converters.getItemStackConverter());
    }

    @Override
    public IStructureModifier<WrappedGameProfile> getGameProfiles() {
        return getModifier(NMS.getGameProfileClass(), Converters.getGameProfileConverter());
    }

    @Override
    public IStructureModifier<PacketDataSerializer> getDataSerializers(boolean copy) {
        return getModifier(NMS.getPacketDataSerializerClass(), Converters.getPacketDataSerializerConverter(), copy);
    }

    @Override
    public IStructureModifier<WrappedBlockPosition> getBlockPositions() {
        return getModifier(NMS.getBlockPositionClass(), Converters.getBlockPositionConverter());
    }

    @Override
    public <T> IStructureModifier<T> getObjectsOf(Class<T> target) {
        return getModifier(target);
    }
}
