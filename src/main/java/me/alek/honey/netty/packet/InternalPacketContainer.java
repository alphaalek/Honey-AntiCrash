package me.alek.honey.netty.packet;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.Reflection;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class InternalPacketContainer<WP extends WrappedPacket<WP>> implements PacketContainer<WP> {

    private PacketTypeEnum type;
    private Object handle;
    private WP wrappedPacket;
    private CallbackAction<PacketContainer<WP>> postAction;
    private final HoneyPlayer player;
    private final Channel channel;
    private ReflectStructure<Object, ?> packetStructure;
    private boolean cancelled;
    private boolean dumped;
    private boolean handledByServer;
    private final long time = System.currentTimeMillis();
    private final PacketEventLoopStateTimings eventLoopStateTracker = new PacketEventLoopStateTimings();
    private final CallbackAction<PacketContainer<WP>> callbackAction = new CallbackAction<>();

    public InternalPacketContainer(Object rawPacket, HoneyPlayer player, Channel channel, CallbackAction<PacketContainer<WP>> postAction, PacketTypeEnum type) {
        this(rawPacket, type, player, channel);
        init(rawPacket, postAction, type);
    }

    public InternalPacketContainer(Object rawPacket, PacketTypeEnum packetType, HoneyPlayer player, Channel channel) {
        this(player, channel);
        this.handle = rawPacket;
        this.type = packetType;
    }

    public InternalPacketContainer(HoneyPlayer player, Channel channel) {
        this.player = player;
        this.channel = channel;
    }

    public void init(Object rawPacket, CallbackAction<PacketContainer<WP>> postAction, PacketTypeEnum packetType) {
        this.handle = rawPacket;
        this.postAction = postAction;
        this.type = packetType;

        if (!PacketType.isUnknown(type) && type.getNmsClass().equals(rawPacket.getClass())) {
            this.packetStructure = ReflectStructureCache.acquireStructure(type);
            if (this.packetStructure.isStaticFieldsAllowed()) {
                this.packetStructure.disableStaticFields();
            }
            this.wrappedPacket = (WP) PacketFactory.createWrappedPacket(type, rawPacket, this);
        }
    }

    @Override
    public String toString() {
        if (type == null) {
            return "Unknown Packet";
        }

        return "Packet:{Type:" + type + ",Player:" + Optional.ofNullable(player).map(p -> p.name).orElse("Unknown") + "}";
    }

    @Override
    public PacketEventLoopStateTimings getEventloopStateTimings() {
        return eventLoopStateTracker;
    }

    @Override
    public long getTime() {
        return this.time;
    }

    @Override
    public void addServerCallbackAction(Consumer<PacketContainer<WP>> action) {
        callbackAction.addAction(action);
    }

    @Override
    public void invokeServerCallback() {
        callbackAction.call(this);
    }

    @Override
    public Channel getChannel() {
        return channel;
    }
    @Override
    public PacketTypeEnum getType() {
        return type;
    }

    @Override
    public PacketState getState() {
        if (type == null) {
            return PacketState.UNKNOWN;
        }

        return type.getState();
    }

    @Override
    public Object getHandle() {
        return handle;
    }

    @Override
    public String getName() {
        return handle.getClass().getSimpleName();
    }

    @Override
    public boolean isDumped() {
        return dumped;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public void dump() {
        this.dumped = true;
    }

    @Override
    public boolean isHandledByServer() {
        return handledByServer;
    }

    @Override
    public void setHandledByServer() {
        this.handledByServer = true;
    }

    @Override
    public CallbackAction<PacketContainer<WP>> getPost() {
        return postAction;
    }

    @Override
    public WP getPacket() {
        return wrappedPacket;
    }

    @Override
    public HoneyPlayer getPlayer() {
        return player;
    }

    public <T> IStructureModifier<T> getModifier(Class<T> clazz) {
        return getModifier(clazz, null);
    }

    public <T, C> IStructureModifier<C> getModifier(Class<T> clazz, JavaConverter<T, C> converter) {
        if (packetStructure == null) {
            return null;
        }

        return packetStructure.withType(clazz, converter).withTarget(handle);
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
    public IStructureModifier<Object> getObjects() {
        return getModifier(Object.class);
    }

    @Override
    public IStructureModifier<Object> getAll() {
        if (packetStructure == null) {
            return null;
        }

        return (IStructureModifier<Object>) packetStructure.withTarget(handle);
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
    public IStructureModifier<WrappedPacketDataSerializer> getDataSerializers() {
        return getModifier(NMS.getPacketDataSerializerClass(), Converters.getPacketDataSerializerConverter());
    }

    @Override
    public IStructureModifier<WrappedBlockPosition> getBlockPositions() {
        return getModifier(NMS.getBlockPositionClass(), Converters.getBlockPositionConverter());
    }

    @Override
    public <T> IStructureModifier<T> getObjects(Class<T> target) {
        if (packetStructure == null) {
            return null;
        }

        return packetStructure.withType(target).withTarget(handle);
    }

}
