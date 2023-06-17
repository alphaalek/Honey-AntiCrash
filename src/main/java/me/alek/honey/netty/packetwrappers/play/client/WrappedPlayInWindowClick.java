package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.Reflection;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInWindowClick extends WrappedPacket<WrappedPlayInWindowClick> {

    private final boolean modern = Protocol.getProtocol().isNewerThanOrEqual(Protocol.v1_17);
    private Class<Enum<?>> enumConstClass;

    public WrappedPlayInWindowClick(Object rawPacket, PacketContainer<WrappedPlayInWindowClick> packetContainer) {
        super(rawPacket, packetContainer);

        if (Protocol.getProtocol().isNewerThanOrEqual(Protocol.v1_9)) {
            enumConstClass = (Class<Enum<?>>) Reflection.getFuzzyClass("{nms}.InventoryClickType", "{nms}.world.inventory.InventoryClickType");
        }
    }

    public int getWindowId() {
        return getContainer().getInts().read(modern ? 1 : 0);
    }

    public void setWindowId(int windowId) {
        getContainer().getInts().write(modern ? 1 : 0, windowId);
    }

    public int getWindowSlot() {
        return getContainer().getInts().read(modern ? 3 : 1);
    }

    public void setWindowSlot(int slot) {
        getContainer().getInts().write(modern ? 3 : 1, slot);
    }

    public int getWindowButton() {
        return getContainer().getInts().read(modern ? 4 : 2);
    }

    public void setWindowButton(int windowButton) {
        getContainer().getInts().write(modern ? 4 : 2, windowButton);
    }

    public int getActionNumber() {
        if (modern) {
            return getContainer().getInts().read(2);
        }
        return getContainer().getShorts().read(0);
    }

    public void setActionNumber(int actionNumber) {
        if (modern) {
            getContainer().getInts().write(2, actionNumber);
        }
        else {
            getContainer().getShorts().write(0, (short) actionNumber);
        }
    }

    public int getMode() {
        if (Protocol.getProtocol().isOlderThanOrEqual(Protocol.v1_8_8)) {
            return getContainer().getInts().read(3);
        }
        Enum<?> enumConst = getContainer().getObjects(enumConstClass).read(0);
        return enumConst.ordinal();
    }

    public void setMode(int mode) {
        if (Protocol.getProtocol().isOlderThanOrEqual(Protocol.v1_8_8)) {
            getContainer().getInts().write(3, mode);
        }
        Enum<?> enumConst = Reflection.getEnumAtIndex(enumConstClass, mode);
        getContainer().getObjects(enumConstClass).write(0, enumConst);
    }

    public ItemStack getItem() {
        return getContainer().getItems().read(0);
    }

    public void setClickedItemStack(ItemStack stack) {
        setClickedItemStack(stack);
    }
}
