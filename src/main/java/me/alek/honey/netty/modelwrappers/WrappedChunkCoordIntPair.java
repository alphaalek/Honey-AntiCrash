package me.alek.honey.netty.modelwrappers;

import me.alek.honey.checks.impl.PositionListener;
import org.bukkit.World;

import java.util.function.Function;

public class WrappedChunkCoordIntPair extends WrappedObject<WrappedChunkCoordIntPair> {

    private final int x;
    private final int z;
    private final Function<World, Boolean> loaded;

    public WrappedChunkCoordIntPair(int x, int z, Function<World, Boolean> loaded) {
        super(null);

        this.x = x;
        this.z = z;
        this.loaded = loaded;
    }

    public WrappedChunkCoordIntPair(Object handle) {
        super(handle);

        this.x = getModifier(int.class).readField("x", 0);
        this.z = getModifier(int.class).readField("z", 0);
        this.loaded = (world) -> PositionListener.isChunkLoaded(world, x, z);
    }

    public boolean isLoaded(World world) {
        return loaded.apply(world);
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public boolean equals(Object object) {
        if (object instanceof WrappedChunkCoordIntPair) {
            WrappedChunkCoordIntPair chunkPair = (WrappedChunkCoordIntPair) object;
            return this.x == chunkPair.getX() && this.z == chunkPair.getZ();
        }
        return false;
    }
}
