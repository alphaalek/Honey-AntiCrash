package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInSetCreativeSlot extends ObjectWrappedPacket<WrappedPlayInSetCreativeSlot> {

    public WrappedPlayInSetCreativeSlot(Object rawPacket, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public ItemStack getItem() {
        return getContainer().getStructure().getItems().readField(0);
    }

    public int getSlot() {
        return getContainer().getStructure().getInts().readField(0);
    }
}
