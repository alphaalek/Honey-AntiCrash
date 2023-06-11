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

    @Override
    public PacketModifier<Double> getDoubles() {
        return packetStructure.withType(double.class).withTarget(handle);
    }

    @Override
    public PacketModifier<Long> getLongs() {
        return packetStructure.withType(long.class).withTarget(handle);
    }

    @Override
    public PacketModifier<Integer> getInts() {
        return packetStructure.withType(int.class).withTarget(handle);
    }

    @Override
    public PacketModifier<Short> getShorts() {
        return packetStructure.withType(short.class).withTarget(handle);
    }

    @Override
    public PacketModifier<Float> getFloats() {
        return packetStructure.withType(float.class).withTarget(handle);
    }

    @Override
    public PacketModifier<Byte> getBytes() {
        return packetStructure.withType(byte.class).withTarget(handle);
    }

    @Override
    public PacketModifier<Boolean> getBooleans() {
        return packetStructure.withType(boolean.class).withTarget(handle);
    }

    @Override
    public PacketModifier<String> getStrings() {
        return packetStructure.withType(String.class).withTarget(handle);
    }

    @Override
    public PacketModifier<Object> getObjects(Class<?> target) {
        return packetStructure.withTarget(handle);
    }

}
