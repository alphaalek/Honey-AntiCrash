package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.netty.util.Reflection;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInWindowClick extends WrappedPacket<WrappedPlayInWindowClick> {

    private static final boolean modern = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17_1);
    private Class<Enum<?>> enumConstClass;

    public WrappedPlayInWindowClick(Object rawPacket, PacketContainer<WrappedPlayInWindowClick> packetContainer) {
        super(rawPacket, packetContainer);

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_9)) {
            enumConstClass = (Class<Enum<?>>) Reflection.getUnsureClass("{nms}.InventoryClickType", "{nms}.world.inventory.InventoryClickType");
        }
    }

    public int getWindowId() {
        return getContainer().getInts().readField(0);
    }

    public int getStateId() {
        return !modern ? 0 : getContainer().getInts().readField(1);
    }

    public int getWindowSlot() {
        return getContainer().getInts().readField(modern ? 2 : 1);
    }

    public int getWindowButton() {
        return getContainer().getInts().readField(modern ? 3 : 2);
    }

    public int getActionNumber() {
        return getContainer().getShorts().getAccessors().isEmpty() ? 0 : getContainer().getShorts().readField(0);
    }

    public int getMode() {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            return getContainer().getInts().readField(3);
        }
        Enum<?> enumConst = getContainer().getObjectsOf(enumConstClass).readField(0);
        return enumConst.ordinal();
    }

    public ItemStack getItem() {
        return getContainer().getItems().readField(0);
    }
}
