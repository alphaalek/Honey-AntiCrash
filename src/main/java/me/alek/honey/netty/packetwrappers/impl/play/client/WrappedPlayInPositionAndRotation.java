package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedLocation;
import me.alek.honey.netty.packet.PacketContainer;

public class WrappedPlayInPositionAndRotation extends WrappedPlayInFlying.Positional.Adapter<WrappedPlayInPositionAndRotation> implements WrappedPlayInFlying.Rotational<WrappedPlayInPositionAndRotation> {

    @Property
    private double x;
    @Property
    private double y;
    @Property
    private double z;
    @Property
    private float yaw;
    @Property
    private float pitch;
    private WrappedLocation location;

    public WrappedPlayInPositionAndRotation(PacketDataSerializer buf, PacketContainer<WrappedPlayInPositionAndRotation> packet) {
        super(buf, packet, true, true);
    }

    @Override
    public WrappedLocation getLocation() {
        return this.location;
    }

    @Override
    protected void postInitializing() {
        this.location = new WrappedLocation(this.x, this.y, this.z, this.yaw, this.pitch);
    }

    @Override
    protected void setX(double x) {
        this.x = x;
    }

    @Override
    protected void setY(double y) {
        this.y = y;
    }

    @Override
    protected void setZ(double z) {
        this.z = z;
    }

    @Override
    protected void setYaw(float yaw) {
        this.yaw = yaw;
    }

    @Override
    protected void setPitch(float pitch) {
        this.pitch = pitch;
    }

    @Override
    public double getX() {
        return this.x;
    }

    @Override
    public double getY() {
        return this.y;
    }

    @Override
    public double getZ() {
        return this.z;
    }

    @Override
    public float getYaw() {
        return this.yaw;
    }

    @Override
    public float getPitch() {
        return this.pitch;
    }
}