package me.alek.honey.netty.modelwrappers;

import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.io.Serializable;
import java.util.Objects;

public class WrappedBlockPosition extends WrappedObject<WrappedBlockPosition> implements Cloneable, Serializable {

    public static final WrappedBlockPosition ORIGIN = new WrappedBlockPosition(0, 0, 0);
    public static final WrappedBlockPosition DEFAULT = new WrappedBlockPosition(-1, -1, -1);

    private final int x;
    private final int y;
    private final int z;

    public WrappedBlockPosition(Object handle) {
        super(handle);
        IStructureModifier<Integer> intModifier = getModifier(int.class);

        this.x = intModifier.readField("x", 0);
        this.y = intModifier.readField("y", 0);
        this.z = intModifier.readField("z", 0);
    }

    public WrappedBlockPosition(int x, int y, int z) {
        super(null);
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
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public boolean isInvalid() {
        return NumberConversions.isInvalid(new int[]{x, y, z});
    }

    public Vector toVector() {
        return new Vector(x, y, z);
    }

    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    public double distance(WrappedBlockPosition blockPosition) {
        return distance(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
    }

    public double distance(Location location) {
        return distance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public double distance(double x, double y, double z) {
        if (me.alek.honey.utility.NumberConversions.isInvalid(new double[]{x, y, z})) {
            return 1000000;
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
        return new WrappedBlockPosition(x * factor, y * factor, z * factor);
    }

    public WrappedBlockPosition divide(int divisor) {
        return new WrappedBlockPosition(x / divisor, y / divisor, z / divisor);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof WrappedBlockPosition)) {
            return false;
        }
        WrappedBlockPosition blockPosition = (WrappedBlockPosition) object;
        if (x != blockPosition.getX()) {
            return false;
        }
        if (y != blockPosition.getY()) {
            return false;
        }
        if (z != blockPosition.getZ()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public WrappedBlockPosition clone() {
        return new WrappedBlockPosition(x, y, z);
    }

    @Override
    public String toString() {
        return "BlockPosition{x=" + x + ",y=" + y + ",z=" + z + "}";
    }
}
