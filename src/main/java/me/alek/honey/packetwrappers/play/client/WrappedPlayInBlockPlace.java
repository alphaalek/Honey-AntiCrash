package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.packetwrappers.play.client.common.WrappedAbstractBlockInteraction;
import me.alek.honey.netty.util.NetProtocol;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInBlockPlace extends WrappedAbstractBlockInteraction<WrappedPlayInBlockPlace> {

    private final boolean below_1_9 = NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8);

    public WrappedPlayInBlockPlace(Object rawPacket, PacketContainer<WrappedPlayInBlockPlace> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public ItemStack getItem() {
       return getContainer().getItems().readField(0);
    }

    public WrappedBlockPosition getBlockPosition() {
        return getContainer().getBlockPositions().readField(1);
    }

    public long getTimestamp() {
        return getContainer().getLongs().readField(0);
    }

    public int getPlacedBlockDirection() {
        return below_1_9 ? getContainer().getInts().readField(0) : 0;
    }

    public float getFacingX() {
        return below_1_9 ? getContainer().getFloats().readField(0) : 0;
    }

    public float getFacingY() {
        return below_1_9 ? getContainer().getFloats().readField(1) : 0;
    }

    public float getFacingZ() {
        return below_1_9 ? getContainer().getFloats().readField(2) : 0;
    }
}
