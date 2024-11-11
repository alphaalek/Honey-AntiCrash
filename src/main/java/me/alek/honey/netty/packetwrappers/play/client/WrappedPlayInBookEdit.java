package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.nbt.CompoundTag;
import me.alek.honey.netty.nbt.PrimitiveTag;
import me.alek.honey.netty.nbt.TagType;
import me.alek.honey.netty.nbt.array.ArrayTag;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.util.MaterialMappings;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInBookEdit extends ByteWrappedPacket<WrappedPlayInBookEdit> {

    private static final int PAGE_CHAR_LIMIT = 1024;
    private static final int TITLE_CHAR_LIMIT = 32;
    private static final int MAX_PAGES = 100;

    @Property
    private final int slot;
    @Property
    private final boolean signing;
    @Property
    private final WrappedImmutableItemStack itemStack;

    public WrappedPlayInBookEdit(PacketDataSerializer buf, PacketContainer<WrappedPlayInBookEdit> packet) {
        super(buf, packet);

        NetProtocol protocol = packet.getProtocolByPacketState();

        if (protocol.isOlderThan(NetProtocol.v1_17)) {
            this.itemStack = buf.readItemStack(protocol);
            this.signing = buf.readBoolean();
            this.slot = buf.readVarInt();
        } else {
            this.slot = buf.readVarInt();

            int pageCount = buf.readVarInt();
            if (pageCount > MAX_PAGES) {
                throw new IllegalStateException("Too many pages: " + pageCount + ", limit: " + MAX_PAGES);
            }

            CompoundTag tag = new CompoundTag();
            ArrayTag<String> pages = new ArrayTag<>(TagType.STRING);
            for (int i = 0; i < pageCount; i++) {
                pages.add(new PrimitiveTag<>(TagType.STRING, buf.readString(PAGE_CHAR_LIMIT)));
            }
            tag.put("pages", pages);

            Material material = MaterialMappings.getTypeIfAny("writable-book");
            if (buf.readBoolean()) {
                tag.put("title", new PrimitiveTag<>(TagType.STRING, buf.readString(TITLE_CHAR_LIMIT)));
                tag.put("author", new PrimitiveTag<>(TagType.STRING, packet.getPlayer().name));
                material = Material.WRITTEN_BOOK;
                this.signing = true;
            } else {
                this.signing = false;
            }

            this.itemStack = WrappedImmutableItemStack.fromBukkit(new ItemStack(material));
            this.itemStack.setNBT(tag);
        }
    }

    public int getSlot() {
        return this.slot;
    }

    public boolean isSigning() {
        return this.signing;
    }

    public WrappedImmutableItemStack getItemStack() {
        return this.itemStack;
    }
}
