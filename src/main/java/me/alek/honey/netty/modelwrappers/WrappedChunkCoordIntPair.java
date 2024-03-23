package me.alek.honey.netty.modelwrappers;

import me.alek.honey.checks.impl.PositionListener;
import org.bukkit.World;

import java.util.function.Function;

public class WrappedChunkCoordIntPair extends WrappedObject<WrappedChunkCoordIntPair> {

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

        this.x = getModifier(int.class).readField("x", 0);
        this.y = getModifier(int.class).readField("z", 0);
        this.loaded = (world) -> PositionListener.isChunkLoaded(world, x, y);
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
