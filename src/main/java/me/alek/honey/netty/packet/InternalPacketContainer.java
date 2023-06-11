package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.packet.PacketModifier;
import me.alek.honey.netty.api.packet.PacketStructure;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.cache.PacketWrapperCache;
import me.alek.honey.netty.packet.structure.PacketStructureCache;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.wrappers.WrappedPacket;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Function;

public final class InternalPacketContainer<WP extends WrappedPacket<WP>> implements PacketContainer<WP> {

    public static Function<Object, PacketContainer<? extends WrappedPacket<?>>> SIMPLE_CONTAINER = (packet) -> {
        return new InternalPacketContainer<>(null, null, null, null, null, packet);
    };
    private final PacketTypeEnum type;
    private final Object handle;
    private final WP wrappedPacket;
    private final Runnable postAction;
    private final Player player;
    private final PacketStructure<Object> packetStructure;
    private boolean cancelled = false;

    public InternalPacketContainer(
            Object rawPacket,
            PacketTypeEnum type,
            Player player
    ) {
        this(rawPacket, player, null, type);
    }

    public InternalPacketContainer(
            Object rawPacket,
            Player player,
            Runnable postAction,
            PacketTypeEnum type
    ) {
        this.wrappedPacket = (WP) PacketWrapperCache.getWrapper(type, rawPacket, this);
        this.packetStructure = PacketStructureCache.getStructure(type);
        this.player = player;
        this.type = type;
        this.postAction = postAction;
        this.handle = rawPacket;
    }

    public InternalPacketContainer(
        PacketStructure<Object> packetStructure,
        WP wrappedPacket,
        Player player,
        PacketTypeEnum type,
        Runnable postAction,
        Object rawPacket
    ) {
        this.wrappedPacket = wrappedPacket;
        this.player = player;
        this.packetStructure = packetStructure;
        this.postAction = postAction;
        this.type = type;
        this.handle = rawPacket;
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

    private <T> PacketModifier<T> getModifier(Class<T> clazz) {
        return packetStructure.withType(clazz).withTarget(handle);
    }

    @Override
    public PacketModifier<Double> getDoubles() {
        return getModifier(double.class);
    }

    @Override
    public PacketModifier<Long> getLongs() {
        return getModifier(long.class);
    }

    @Override
    public PacketModifier<Integer> getInts() {
        return getModifier(int.class);
    }

    @Override
    public PacketModifier<Short> getShorts() {
        return getModifier(short.class);
    }

    @Override
    public PacketModifier<Float> getFloats() {
        return getModifier(float.class);
    }

    @Override
    public PacketModifier<Byte> getBytes() {
        return getModifier(byte.class);
    }

    @Override
    public PacketModifier<Boolean> getBooleans() {
        return getModifier(boolean.class);
    }

    @Override
    public PacketModifier<String> getStrings() {
        return getModifier(String.class);
    }

    @Override
    public PacketModifier<Object> getObjects() {
        return getModifier(Object.class);
    }

    @Override
    public PacketModifier<UUID> getUUIDS() {
        return getModifier(UUID.class);
    }

    @Override
    public PacketModifier<String[]> getStringArrays() {
        return getModifier(String[].class);
    }

    @Override
    public PacketModifier<long[]> getLongArrays() {
        return getModifier(long[].class);
    }

    @Override
    public PacketModifier<int[]> getIntArrays() {
        return getModifier(int[].class);
    }

    @Override
    public PacketModifier<short[]> getShortArrays() {
        return getModifier(short[].class);
    }

    @Override
    public PacketModifier<byte[]> getByteArrays() {
        return getModifier(byte[].class);
    }

    @Override
    public <T> PacketModifier<T> getObjects(Class<T> target) {
        return packetStructure.withType(target).withTarget(handle);
    }

}
