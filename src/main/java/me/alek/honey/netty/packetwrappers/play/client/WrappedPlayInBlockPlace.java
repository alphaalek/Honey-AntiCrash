package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInBlockPlace extends WrappedPacket<WrappedPlayInBlockPlace> {


    public WrappedPlayInBlockPlace(Object rawPacket, PacketContainer<WrappedPlayInBlockPlace> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public ItemStack getItem() {
       return getContainer().getItems().read(0);
    }

    public WrappedBlockPosition getBlockPosition() {
        return getContainer().getBlockPositions().read(1);
    }

    public long getTimestamp() {
        return getContainer().getLongs().read(0);
    }

    // < 1.9
    public int getPlacedBlockDirection() {
        return getContainer().getInts().read(0);
    }

    // < 1.9
    public float getFacingX() {
        return getContainer().getFloats().read(0);
    }

    // < 1.9
    public float getFacingY() {
        return getContainer().getFloats().read(1);
    }

    // < 1.9
    public float getFacingZ() {
        return getContainer().getFloats().read(2);
    }


}
