package me.alek.honey.netty.modelwrappers;

import org.bukkit.Location;
import org.bukkit.World;

public class WrappedLocation {

    public static final WrappedLocation ORIGIN = new WrappedLocation();

    private double x;
    private double y;
    private double z;

    private float yaw;
    private float pitch;

    public WrappedLocation(Location location) {
        this(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public WrappedLocation(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public WrappedLocation(double x, double y, double z) {
        this(x, y, z, 0, 0);
    }

    public WrappedLocation() {
        this(0, 0, 0, 0, 0);
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public Location toBukkit(World world) {
        return new Location(world, this.x, this.y, this.z, this.yaw, this.pitch);
    }
}
