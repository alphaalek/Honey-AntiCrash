package me.alek.honey.netty.packet;

import io.netty.channel.Channel;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.function.Function;

public final class InternalPacketContainer<WP extends WrappedPacket<WP>> implements PacketContainer<WP> {

    private static final Class<Object> packetDataSerializerClass = Reflection.getFuzzyObjectClass("{nms}.PacketDataSerializer", "{nms}.network.PacketDataSerializer");

    public static Function<Object, PacketContainer<? extends WrappedPacket<?>>> SIMPLE_CONTAINER = (packet) -> {
        return new InternalPacketContainer<>(packet, null, null, null, null, null, null);
    };
    private final PacketTypeEnum type;
    private final Object handle;
    private final WP wrappedPacket;
    private final Runnable postAction;
    private final Player player;
    private final Channel channel;
    private final ReflectStructure<Object, ?> packetStructure;
    private boolean cancelled = false;

    public InternalPacketContainer(Object rawPacket, Player player, Channel channel, Runnable postAction, PacketTypeEnum type) {
        this.player = player;
        this.type = type;
        this.postAction = postAction;
        this.handle = rawPacket;
        this.channel = channel;
        this.packetStructure = ReflectStructureCache.acquireStructure(type);
        this.wrappedPacket = (WP) PacketWrapperInvokerCache.getWrapper(type, rawPacket, this);
    }

    public InternalPacketContainer(Object rawPacket, Player player, Channel channel, Runnable postAction, PacketTypeEnum type, ReflectStructure<Object, Object> packetStructure, WP wrappedPacket) {
        this.wrappedPacket = wrappedPacket;
        this.player = player;
        this.packetStructure = packetStructure;
        this.postAction = postAction;
        this.type = type;
        this.handle = rawPacket;
        this.channel = channel;
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
        return type.getState();
    }

    @Override
    public Object getHandle() {
        return handle;
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
    public Runnable getPost() {
        return postAction;
    }

    @Override
    public WP getPacket() {
        return wrappedPacket;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public <T> IStructureModifier<T> getModifier(Class<T> clazz) {
        return getModifier(clazz, null);
    }

    public <T, C> IStructureModifier<C> getModifier(Class<T> clazz, JavaConverter<T, C> converter) {
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
    public IStructureModifier<?> getAll() {
        return packetStructure.withTarget(handle);
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
        return getModifier(NMSUtils.getItemStackClass(), Converters.getItemstackConverter());
    }

    @Override
    public IStructureModifier<WrappedGameProfile> getGameProfiles() {
        return getModifier(NMSUtils.getGameProfileClass(), Converters.getGameProfileConverter());
    }

    @Override
    public IStructureModifier<WrappedPacketDataSerializer> getDataSerializers() {
        return getModifier(packetDataSerializerClass, Converters.getPacketDataSerializerConverter());
    }

    @Override
    public IStructureModifier<WrappedBlockPosition> getBlockPositions() {
        return getModifier(NMSUtils.getBlockPositionClass(), Converters.getBlockPositionConverter());
    }

    @Override
    public <T> IStructureModifier<T> getObjects(Class<T> target) {
        return packetStructure.withType(target).withTarget(handle);
    }

}
