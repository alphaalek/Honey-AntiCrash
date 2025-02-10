package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedLocation;
import me.alek.honey.netty.packet.PacketContainer;

public class WrappedPlayInPosition extends WrappedPlayInFlying.Positional.Adapter<WrappedPlayInPosition> {

    @Property
    private double x;
    @Property
    private double y;
    @Property
    private double z;
    private WrappedLocation location;

    public WrappedPlayInPosition(PacketDataSerializer buf, PacketContainer<WrappedPlayInPosition> packetContainer) {
        super(buf, packetContainer, true, false);
    }

    @Override
    public WrappedLocation getLocation() {
        return this.location;
    }

    @Override
    protected void postInitializing() {
        this.location = new WrappedLocation(this.x, this.y, this.z);
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
}
