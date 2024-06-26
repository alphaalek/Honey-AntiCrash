package me.alek.honey.netty.modelwrappers;

import me.alek.honey.validation.impl.MovementValidations;
import me.alek.honey.util.reflection.FieldAccessor;
import org.bukkit.World;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class WrappedChunkCoordIntPair extends WrappedObject<WrappedChunkCoordIntPair> {

    private static final AtomicReference<FieldAccessor<Integer>> chunkCoordIntPairXCachedAccessor = new AtomicReference<>();
    private static final AtomicReference<FieldAccessor<Integer>> chunkCoordIntPairYCachedAccessor = new AtomicReference<>();

    private final int x;
    private final int y;
    private final Function<World, Boolean> loaded;

    public WrappedChunkCoordIntPair(int x, int y, Function<World, Boolean> loaded) {
        super(null);

        this.x = x;
        this.y = y;
        this.loaded = loaded;
    }

    public WrappedChunkCoordIntPair(Object handle) {
        super(handle);

        this.x = getModifier(int.class).readField(chunkCoordIntPairXCachedAccessor, "x", 0);
        this.y = getModifier(int.class).readField(chunkCoordIntPairYCachedAccessor, "z", 0);
        this.loaded = (world) -> MovementValidations.isChunkLoaded(world, x, y);
    }

    @Override
    public String toString() {
        return x + "," + y;
    }

    public boolean isLoaded(World world) {
        return loaded.apply(world);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean equals(Object object) {
        if (object instanceof WrappedChunkCoordIntPair) {
            WrappedChunkCoordIntPair chunkPair = (WrappedChunkCoordIntPair) object;
            return this.x == chunkPair.getX() && this.y == chunkPair.getY();
        }
        return false;
    }
}
