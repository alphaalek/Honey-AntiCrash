package me.alek.honey.netty.packetwrappers.play.client;

import io.netty.buffer.Unpooled;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packetwrappers.AutoReleasable;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.Reflection;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

public class WrappedPlayInWindowClick extends ByteWrappedPacket<WrappedPlayInWindowClick> implements AutoReleasable {

    public enum WindowClickType {
        PICKUP, QUICK_MOVE, SWAP, CLONE, THROW, QUICK_CRAFT, PICKUP_ALL;
    }

    private final int windowId;
    private int stateId;
    private final int slot;
    private final int button;
    private int actionNumber;
    private final WindowClickType windowClickType;
    private Map<Integer, ItemStack> slots;
    private final LazyInit<ItemStack> carriedItemStack;

    private final Runnable release;

    public WrappedPlayInWindowClick(PacketDataSerializer serializer, PacketContainer<WrappedPlayInWindowClick> packetContainer) {
        super(serializer, packetContainer);

        // TODO
        if (false /*NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_21_2) */) {
            this.windowId = serializer.readVarInt();
        } else {
            this.windowId = serializer.readUnsignedByte();
        }

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17_1)) {
            this.stateId = serializer.readVarInt();
        }
        this.slot = serializer.readShort();
        this.button = serializer.readByte();
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17)) {
            this.actionNumber = serializer.readShort();
        }

        int clickTypeIndex = serializer.readVarInt();
        this.windowClickType = WindowClickType.values()[clickTypeIndex];
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17)) {
            this.slots = serializer.readMap(
                    serializerInput -> Math.toIntExact(serializerInput.readShort()),
                    PacketDataSerializer::readBukkitItemStack
            );
        }

        PacketDataSerializer copy = new PacketDataSerializer(Unpooled.copiedBuffer(serializer));
        this.carriedItemStack = new LazyInit<>(copy::readBukkitItemStack);
        release = () -> AutoReleasable.doRelease(copy);
    }

    @Override
    public void release() {
        this.release.run();
    }

    public int getWindowId() {
        return windowId;
    }

    public int getStateId() {
        return this.stateId;
    }

    public int getSlot() {
        return this.slot;
    }

    public int getButton() {
        return this.button;
    }

    public int getActionNumber() {
        return this.actionNumber;
    }

    public WindowClickType getWindowClickType() {
        return this.windowClickType;
    }

    public Map<Integer, ItemStack> getSlots() {
        return this.slots;
    }

    public ItemStack getCarriedItemStack() {
        return this.carriedItemStack.get();
    }
}
