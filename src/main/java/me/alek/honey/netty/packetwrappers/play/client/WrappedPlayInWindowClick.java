package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.Reflection;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInWindowClick extends ObjectWrappedPacket<WrappedPlayInWindowClick> {

    private static final boolean modern = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17_1);
    private Class<Enum<?>> enumConstClass;

    public WrappedPlayInWindowClick(Object rawPacket, PacketContainer<WrappedPlayInWindowClick> packetContainer) {
        super(rawPacket, packetContainer);

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_9)) {
            enumConstClass = Reflection.getUnsureClass("{nms}.InventoryClickType", "{nms}.world.inventory.InventoryClickType");
        }
    }

    public int getWindowId() {
        return getContainer().getStructure().getInts().readField(0);
    }

    public int getStateId() {
        return !modern ? 0 : getContainer().getStructure().getInts().readField(1);
    }

    public int getWindowSlot() {
        return getContainer().getStructure().getInts().readField(modern ? 2 : 1);
    }

    public int getWindowButton() {
        return getContainer().getStructure().getInts().readField(modern ? 3 : 2);
    }

    public int getActionNumber() {
        return getContainer().getStructure().getShorts().getAccessors().isEmpty() ? 0 : getContainer().getStructure().getShorts().readField(0);
    }

    public int getMode() {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            return getContainer().getStructure().getInts().readField(3);
        }
        Enum<?> enumConst = getContainer().getStructure().getObjectsOf(enumConstClass).readField(0);
        return enumConst.ordinal();
    }

    public ItemStack getItem() {
        return getContainer().getStructure().getItems().readField(0);
    }
}
