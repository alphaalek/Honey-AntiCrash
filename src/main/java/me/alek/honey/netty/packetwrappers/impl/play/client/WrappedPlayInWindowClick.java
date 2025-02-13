package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacketImpl;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.Map;
import java.util.stream.Collectors;

public class WrappedPlayInWindowClick extends ByteInWrappedPacketImpl.CacheRewriteCaller<WrappedPlayInWindowClick> {

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
            // Note, that I'm using ByteInWrappedPacket#serializer() because if any prior item stacks
            // has rewritten the serializer, I have to handle the continuation properly
            this.slots = serializer.readMap(
                    () -> Math.toIntExact(serializer().readShort()),
                    () -> {
                        Rewritable<WrappedImmutableItemStack> rewritable = serializer().readItemStackSafely(packet);
                        handleRewritten(rewritable); // update rewritten serializer
                        return rewritable.get();
                    }
            );
        }
        // Note, same as when reading the item stack slots
        Rewritable<WrappedImmutableItemStack> rewritable = serializer().readItemStackSafely(packet, 0);
        handleRewritten(rewritable); // update rewritten serializer
        this.carriedItemStack = rewritable.get();
    }

    @Override
    protected boolean acceptsRewriting(PacketContainer<?> packet) {
        return !ConfigurationOptions.Decoder.DISABLE_ITEM_TAG_WINDOW_CLICK && super.acceptsRewriting(packet);
    }

    @Override
    public boolean shouldRewrite(PacketContainer<?> packet) {
        return ConfigurationOptions.Decoder.DISABLE_ITEM_TAG_WINDOW_CLICK && packet.getLastRewrittenPipelineState() == null
                || super.shouldRewrite(packet);
    }

    @Override
    public void rewrite(PacketDataSerializer serializer) {
        if (ConfigurationOptions.Decoder.DISABLE_ITEM_TAG_WINDOW_CLICK) {
            rewriteSanitized(serializer);
        } else {
            super.rewrite(serializer);
        }
    }

    private void rewriteSanitized(PacketDataSerializer serializer) {
        NetProtocol protocol = getProtocol();

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
            Map<Integer, WrappedImmutableItemStack> sanitizedSlots = this.slots.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> new WrappedImmutableItemStack(getProtocol(), entry.getValue().getType())));
            serializer.writeMap(sanitizedSlots,
                    serializer::writeShort,
                    stack -> serializer.writeItemStack(getContainer(), stack)
            );
        }

        serializer.writeItemStack(getContainer(), new WrappedImmutableItemStack(getProtocol(), this.carriedItemStack.getType()));
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
