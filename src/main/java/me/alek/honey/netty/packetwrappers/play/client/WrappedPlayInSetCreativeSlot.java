package me.alek.honey.netty.packetwrappers.play.client;

import io.netty.buffer.Unpooled;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packetwrappers.AutoReleasable;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.protocol.NetProtocol;

public class WrappedPlayInSetCreativeSlot extends ByteWrappedPacket<WrappedPlayInSetCreativeSlot> {

    @Property
    private final int slot;
    @Property
    private final WrappedImmutableItemStack item;

    public WrappedPlayInSetCreativeSlot(PacketDataSerializer serializer, PacketContainer<WrappedPlayInSetCreativeSlot> packet) {
        super(serializer, packet);

        NetProtocol protocol = packet.getProtocolByBytePacketState();

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            this.slot = serializer.readUnsignedShort();
        } else {
            this.slot = serializer.readShort();
        }

        PacketDataSerializer copy = new PacketDataSerializer(Unpooled.copiedBuffer(serializer));
        this.item = copy.readItemStack(protocol);
    }

    public int getSlot() {
        return this.slot;
    }

    public WrappedImmutableItemStack getItemStack() {
        return this.item;
    }
}
