package me.alek.honey.netty.modelwrappers;

import me.alek.honey.netty.utility.reflect.NMSUtils;

public class WrappedChunkCoordIntPair extends WrappedObject<WrappedChunkCoordIntPair> {

    private final int x;
    private final int z;

    public WrappedChunkCoordIntPair(int x, int z) {
        super(null);

        this.x = x;
        this.z = z;
    }

    public WrappedChunkCoordIntPair(Object handle) {
        super(handle);

        this.x = getModifier(int.class).readField("x", 0);
        this.z = getModifier(int.class).readField("z", 0);
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof WrappedChunkCoordIntPair) {
            WrappedChunkCoordIntPair chunkPair = (WrappedChunkCoordIntPair) object;
            return this.x == chunkPair.getX() && this.z == chunkPair.getZ();
        }
        if (NMSUtils.getChunkCoordIntPairClass().isInstance(object)) {
            return equals(new WrappedChunkCoordIntPair(object));
        }
        return false;
    }
}
