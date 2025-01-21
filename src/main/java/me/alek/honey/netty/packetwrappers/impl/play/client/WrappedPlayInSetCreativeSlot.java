package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;

public class WrappedPlayInSetCreativeSlot extends ByteWrappedPacket<WrappedPlayInSetCreativeSlot> {

    @Property
    private final int slot;
    @Property
    private final WrappedImmutableItemStack item;

    public WrappedPlayInSetCreativeSlot(PacketDataSerializer serializer, PacketContainer<WrappedPlayInSetCreativeSlot> packet) {
        super(serializer, packet);

        NetProtocol protocol = packet.getProtocol();

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            this.slot = serializer.readUnsignedShort();
        } else {
            this.slot = serializer.readShort();
        }

        this.item = serializer.readItemStack(protocol);
    }

    public int getSlot() {
        return this.slot;
    }

    public WrappedImmutableItemStack getItemStack() {
        return this.item;
    }
}
