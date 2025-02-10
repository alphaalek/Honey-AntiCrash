package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;

public class WrappedPlayInRotation extends WrappedPlayInFlying<WrappedPlayInRotation> implements WrappedPlayInFlying.Rotational<WrappedPlayInRotation> {

    @Property
    private float yaw;
    @Property
    private float pitch;

    public WrappedPlayInRotation(PacketDataSerializer buf, PacketContainer<WrappedPlayInRotation> packetContainer) {
        super(buf, packetContainer, false, true);
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
    public float getYaw() {
        return this.yaw;
    }

    @Override
    public float getPitch() {
        return this.pitch;
    }
}
