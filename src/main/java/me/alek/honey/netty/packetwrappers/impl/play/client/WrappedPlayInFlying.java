package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedLocation;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacketImpl;

public abstract class WrappedPlayInFlying<FWP extends WrappedPlayInFlying<FWP>> extends ByteInWrappedPacketImpl<FWP> {

    public static class Flying extends Positional.Adapter<Flying> {

        public Flying(PacketDataSerializer buf, PacketContainer<Flying> packet) {
            super(buf, packet, false, false);
        }

        @Override
        public WrappedLocation getLocation() {
            return WrappedLocation.ORIGIN;
        }
    }

    public interface Positional<WP extends Positional<WP>> extends ByteInWrappedPacket<WP> {

        abstract class Adapter<WP extends Adapter<WP>> extends WrappedPlayInFlying<WP> implements Positional<WP> {

            @Property
            private boolean onGround;

            public Adapter(PacketDataSerializer buf, PacketContainer<WP> packet, boolean hasPos, boolean hasRot) {
                super(buf, packet, hasPos, hasRot);
            }

            @Override
            protected void setOnGround(boolean onGround) {
                this.onGround = onGround;
            }

            @Override
            public boolean isOnGround() {
                return this.onGround;
            }

            @Override
            public double getX() {
                return getLocation().getX();
            }

            @Override
            public double getY() {
                return getLocation().getY();
            }

            @Override
            public double getZ() {
                return getLocation().getZ();
            }
        }

        WrappedLocation getLocation();

        double getX();

        double getY();

        double getZ();

        boolean isOnGround();
    }

    public interface Rotational<WP extends Rotational<WP>> extends ByteInWrappedPacket<WP> {

        float getYaw();

        float getPitch();
    }

    private static final byte GROUND_MARK = 0x01;
    private static final byte HORIZONTAL_COLLISION_MARK = 0x01;

    public WrappedPlayInFlying(PacketDataSerializer buf, PacketContainer<FWP> packet, boolean hasPos, boolean hasRot) {
        super(buf, packet);
        if (hasPos) {
            setX(buf.readDouble());
            setY(buf.readDouble());
            setZ(buf.readDouble());
        }
        if (hasRot) {
            setYaw(buf.readFloat());
            setPitch(buf.readFloat());
        }
        byte flags = buf.readByte();
        setOnGround((flags & GROUND_MARK) > 0);
        setHorizontalCollision((flags & HORIZONTAL_COLLISION_MARK) > 0);

        postInitializing();
    }

    protected void setX(double x) {

    }

    protected void setY(double y) {

    }

    protected void setZ(double z) {

    }

    protected void setYaw(float yaw) {

    }

    protected void setPitch(float pitch) {

    }

    protected void setOnGround(boolean onGround) {

    }

    protected void setHorizontalCollision(boolean horizontalCollision) {

    }

    protected void postInitializing() {

    }
}
