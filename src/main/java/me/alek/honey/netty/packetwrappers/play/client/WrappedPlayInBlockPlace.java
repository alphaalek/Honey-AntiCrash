package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInBlockPlace extends ObjectWrappedPacket<WrappedPlayInBlockPlace> {

    private final boolean below_1_9 = NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8);

    public WrappedPlayInBlockPlace(Object rawPacket, PacketContainer<WrappedPlayInBlockPlace> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public ItemStack getItem() {
       return getContainer().getStructure().getItems().readField(0);
    }

    public WrappedBlockPosition getBlockPosition() {
        return getContainer().getStructure().getBlockPositions().readField(1);
    }

    public long getTimestamp() {
        return getContainer().getStructure().getLongs().readField(0);
    }

    public int getPlacedBlockDirection() {
        return below_1_9 ? getContainer().getStructure().getInts().readField(0) : 0;
    }

    public float getFacingX() {
        return below_1_9 ? getContainer().getStructure().getFloats().readField(0) : 0;
    }

    public float getFacingY() {
        return below_1_9 ? getContainer().getStructure().getFloats().readField(1) : 0;
    }

    public float getFacingZ() {
        return below_1_9 ? getContainer().getStructure().getFloats().readField(2) : 0;
    }
}
