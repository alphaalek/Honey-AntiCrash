package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacketImpl;

public class WrappedPlayInCreativeInventoryAction extends ByteInWrappedPacketImpl.CacheRewriteCaller<WrappedPlayInCreativeInventoryAction> {

    @Property
    private final int slot;
    @Property
    private final WrappedImmutableItemStack item;

    public WrappedPlayInCreativeInventoryAction(PacketDataSerializer serializer, PacketContainer<WrappedPlayInCreativeInventoryAction> packet) {
        super(serializer, packet);

        this.slot = serializer.readShort();
        Rewritable<WrappedImmutableItemStack> rewritable = serializer.readItemStackSafely(packet, 0);
        handleRewritten(rewritable);
        this.item = rewritable.get();
    }

    public int getSlot() {
        return this.slot;
    }

    public WrappedImmutableItemStack getItemStack() {
        return this.item;
    }
}
