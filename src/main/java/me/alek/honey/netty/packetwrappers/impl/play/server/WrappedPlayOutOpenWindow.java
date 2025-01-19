package me.alek.honey.netty.packetwrappers.impl.play.server;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;

public class WrappedPlayOutOpenWindow extends ObjectWrappedPacket<WrappedPlayOutOpenWindow> {

    public WrappedPlayOutOpenWindow(Object rawPacket, PacketContainer<WrappedPlayOutOpenWindow> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public int getWindowID() {
        return getContainer().getStructure().getInts().readField(0);
    }

    public Container getInventoryContainer() {
        // TODO: fix
        return null;
    }

    public enum Container {
        PLAYER,
        CHEST1,
        CHEST2,
        CHEST3,
        CHEST4,
        CHEST5,
        CHEST6,
        DISPENSER,
        CRAFTER,
        ANVIL,
        BEACON,
        BLAST_FURNACE,
        FURNACE,
        BREWING_STAND,
        WORKBENCH,
        ENCHANTMENT_TABLE,
        GRINDSTONE,
        HOPPER,
        LECTERN,
        LOOM,
        MERCHANT,
        SHULKER_BOX,
        SMITHING_TABLE,
        SMOKER,
        CARTOGRAPHY_TABLE,
        STONECUTTER
        ;
    }
}
