package me.alek.honey.netty.modelwrappers;

import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.FieldAccessor;
import me.alek.honey.util.reflection.MinecraftReflection;
import me.alek.honey.util.reflection.Reflection;
import me.alek.honey.util.statics.MathHelper;
import me.alek.honey.util.statics.NumberUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class WrappedBlockPosition extends WrappedObject<WrappedBlockPosition> implements Cloneable, Serializable {

    private static final ConstructorInvoker<?> nmsConstructorInvoker = Reflection.getConstructor(MinecraftReflection.getBlockPositionClass(), int.class, int.class, int.class);
    public static final WrappedBlockPosition ORIGIN = new WrappedBlockPosition(0, 0, 0);

    private static final AtomicReference<FieldAccessor<Integer>> blockPositionXCachedAccessor = new AtomicReference<>();
    private static final AtomicReference<FieldAccessor<Integer>> blockPositionYCachedAccessor = new AtomicReference<>();
    private static final AtomicReference<FieldAccessor<Integer>> blockPositionZCachedAccessor = new AtomicReference<>();

    private static final int c = 1 + MathHelper.c(MathHelper.b(30000000));
    private static final int d = c;
    private static final int e = 64 - c - d;
    private static final int f = d;
    private static final int g = f + e;
    private static final long h = (1L << c) - 1L;
    private static final long i = (1L << e) - 1L;
    private static final long j = (1L << d) - 1L;

    private final int x;
    private final int y;
    private final int z;

    public static WrappedBlockPosition fromLong(long val) {
        int x = (int)(val << 64 - g - c >> 64 - c);
        int y = (int)(val << 64 - f - e >> 64 - e);
        int z = (int)(val << 64 - d >> 64 - d);
        return new WrappedBlockPosition(x, y, z);
    }

    public WrappedBlockPosition(Object handle) {
        super(handle);
        IStructureModifier<Integer> intModifier = getModifier(int.class);

        this.x = intModifier.readField(blockPositionXCachedAccessor, "x", 0);
        this.y = intModifier.readField(blockPositionYCachedAccessor, "y", 0);
        this.z = intModifier.readField(blockPositionZCachedAccessor, "z", 0);
    }

    public WrappedBlockPosition(int x, int y, int z) {
        super(nmsConstructorInvoker.invoke(x, y, z));
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public WrappedBlockPosition(double x, double y, double z) {
        this((int) x, (int) y, (int) z);
    }

    public WrappedBlockPosition(Location location) {
        this(location.getX(), location.getY(), location.getZ());
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public boolean isInvalid() {
        return NumberUtils.isInvalid(this.x, this.y, this.z);
    }

    public Vector toVector() {
        return new Vector(this.x, this.y, this.z);
    }

    public Location toLocation(World world) {
        return new Location(world, this.x, this.y, this.z);
    }

    public double distance(WrappedBlockPosition blockPosition) {
        return distance(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
    }

    public double distance(Location location) {
        return distance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public double distance(WrappedLocation location) {
        return distance(location.getX(), location.getY(), location.getZ());
    }

    public double distance(double x, double y, double z) {
        if (NumberUtils.isInvalid(x, y, z)) {
            return -1;
        }
        double deltaX = Math.abs(this.x - x);
        double deltaY = Math.abs(this.y - y);
        double deltaZ = Math.abs(this.z - z);
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2) + Math.pow(deltaZ, 2));
    }

    public WrappedBlockPosition add(WrappedBlockPosition blockPosition) {
        return add(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
    }

    public WrappedBlockPosition add(Vector vector) {
        return add(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    public WrappedBlockPosition add(int x, int y, int z) {
        return new WrappedBlockPosition(this.x + x, this.y + y, this.z + z);
    }

    public WrappedBlockPosition subtract(WrappedBlockPosition blockPosition) {
        return subtract(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
    }

    public WrappedBlockPosition subtract(Vector vector) {
        return subtract(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    public WrappedBlockPosition subtract(int x, int y, int z) {
        return new WrappedBlockPosition(this.x - x, this.y - y, this.z - z);
    }

    public WrappedBlockPosition multiply(int factor) {
        return new WrappedBlockPosition(this.x * factor, this.y * factor, this.z * factor);
    }

    public WrappedBlockPosition divide(int divisor) {
        return new WrappedBlockPosition(this.x / divisor, this.y / divisor, this.z / divisor);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof WrappedBlockPosition)) {
            return false;
        }
        WrappedBlockPosition blockPosition = (WrappedBlockPosition) object;
        if (this.x != blockPosition.getX()) {
            return false;
        }
        if (this.y != blockPosition.getY()) {
            return false;
        }
        if (this.z != blockPosition.getZ()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z);
    }

    @Override
    public WrappedBlockPosition clone() {
        return new WrappedBlockPosition(this.x, this.y, this.z);
    }

    @Override
    public String toString() {
        return this.x + "," + this.y + "," + this.z;
    }
}
