package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.checks.helper.ItemHelper;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.Reflection;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
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
        return getPacket().getInts().read(modern ? 1 : 0);
    }

    public void setWindowId(int windowId) {
        getPacket().getInts().write(modern ? 1 : 0, windowId);
    }

    public int getWindowSlot() {
        return getPacket().getInts().read(modern ? 3 : 1);
    }

    public void setWindowSlot(int slot) {
        getPacket().getInts().write(modern ? 3 : 1, slot);
    }

    public int getWindowButton() {
        return getPacket().getInts().read(modern ? 4 : 2);
    }

    public void setWindowButton(int windowButton) {
        getPacket().getInts().write(modern ? 4 : 2, windowButton);
    }

    public int getActionNumber() {
        if (modern) {
            return getPacket().getInts().read(2);
        }
        return getPacket().getShorts().read(0);
    }

    public void setActionNumber(int actionNumber) {
        if (modern) {
            getPacket().getInts().write(2, actionNumber);
        }
        else {
            getPacket().getShorts().write(0, (short) actionNumber);
        }
    }

    public int getMode() {
        if (Protocol.getProtocol().isOlderThanOrEqual(Protocol.v1_8_8)) {
            return getPacket().getInts().read(3);
        }
        Bukkit.broadcastMessage(enumConstClass + "");
        Enum<?> enumConst = getPacket().getObjects(enumConstClass).read(0);
        Bukkit.broadcastMessage(enumConst + "");
        Bukkit.broadcastMessage(enumConst.ordinal() + "");
        return enumConst.ordinal();
    }

    public void setMode(int mode) {
        if (Protocol.getProtocol().isOlderThanOrEqual(Protocol.v1_8_8)) {
            getPacket().getInts().write(3, mode);
        }
        Enum<?> enumConst = Reflection.getEnumAtIndex(enumConstClass, mode);
        getPacket().getObjects(enumConstClass).write(0, enumConst);
    }

    public Object getItem() {
        return getPacket().getItems().read(0);
    }

    public void setClickedItemStack(ItemStack stack) {
        setClickedItemStack(ItemHelper.asNMSItem(stack));
    }

    public void setClickedItemStack(Object stack) {
        getPacket().getItems().write(0, stack);
    }
}
