package me.alek.honey.netty.packetwrappers.impl.play.client;

import io.netty.buffer.ByteBuf;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.util.Map;

public class WrappedPlayInWindowClick extends ByteInWrappedPacket<WrappedPlayInWindowClick> {

    public enum WindowClickType {
        PICKUP, QUICK_MOVE, SWAP, CLONE, THROW, QUICK_CRAFT, PICKUP_ALL;
    }

    @Property
    private final int windowId;
    @Property(from = @NetProtocol.From(from = NetProtocol.v1_17_1))
    private int stateId;
    @Property
    private final int slot;
    @Property
    private final int button;
    @Property(to = @NetProtocol.To(to = NetProtocol.v1_16_5))
    private int actionNumber;
    @Property
    private final WindowClickType windowClickType;
    @Property(from = @NetProtocol.From(from = NetProtocol.v1_17))
    private Map<Integer, WrappedImmutableItemStack> slots;
    @Property
    private final WrappedImmutableItemStack carriedItemStack;

    public WrappedPlayInWindowClick(PacketDataSerializer serializer, PacketContainer<WrappedPlayInWindowClick> packet) {
        super(serializer, packet);
        NetProtocol protocol = getProtocol();

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_21_2)) {
            this.windowId = serializer.readVarInt();
        } else {
            this.windowId = serializer.readUnsignedByte();
        }

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_17_1)) {
            this.stateId = serializer.readVarInt();
        }
        this.slot = serializer.readShort();
        this.button = serializer.readByte();
        if (protocol.isOlderThan(NetProtocol.v1_17)) {
            this.actionNumber = serializer.readShort();
        }

        int clickTypeIndex = serializer.readVarInt();
        this.windowClickType = WindowClickType.values()[clickTypeIndex];
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_17)) {
            this.slots = serializer.readMap(
                    () -> Math.toIntExact(serializer.readShort()),
                    () -> serializer.readItemStackSafely(packet)
            );
        }

        this.carriedItemStack = serializer.readItemStackSafely(packet, 0);
    }

    public ByteBuf constructModifiedBuf(NetProtocol protocol) {
        PacketDataSerializer serializer = new PacketDataSerializer();
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_21_2)) {
            serializer.writeVarInt(this.windowId);
        } else {
            serializer.writeByte(this.windowId);
        }

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_17_1)) {
            serializer.writeVarInt(this.stateId);
        }
        serializer.writeShort(this.slot);
        serializer.writeByte(this.button);
        if (protocol.isOlderThanOrEqual(NetProtocol.v1_17)) {
            serializer.writeShort(this.actionNumber);
        }

        serializer.writeVarInt(this.windowClickType.ordinal());
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_17)) {
            serializer.writeMap(this.slots,
                    serializer::writeShort,
                    stack -> serializer.writeItemStack(protocol, WrappedImmutableItemStack.AIR)
            );
        }

        serializer.writeItemStack(protocol, WrappedImmutableItemStack.AIR);
        return serializer;
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

    public Map<Integer, WrappedImmutableItemStack> getSlots() {
        return this.slots;
    }

    public WrappedImmutableItemStack getCarriedItemStack() {
        return this.carriedItemStack;
    }
}
