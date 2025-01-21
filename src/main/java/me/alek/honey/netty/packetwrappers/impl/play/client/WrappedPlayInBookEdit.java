package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.validation.CodecType;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.nbt.CompoundTag;
import me.alek.honey.netty.nbt.PrimitiveTag;
import me.alek.honey.netty.nbt.TagType;
import me.alek.honey.netty.nbt.array.ArrayTag;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.netty.packetwrappers.protocol.InteractionHand;
import me.alek.honey.util.MaterialMappings;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInBookEdit extends ByteWrappedPacket<WrappedPlayInBookEdit> {

    public static final int PAGE_CHAR_LIMIT = 1024;
    public static final int TITLE_CHAR_LIMIT = 16;
    public static final int AUTHOR_CHAR_LIMIT = 16;
    public static final int MAX_PAGES = 100;
    public static final int MAX_PAGES_1_13_2 = 50;

    @Property
    private final InteractionHand interactionHand;
    @Property
    private final boolean signing;
    @Property
    private final WrappedImmutableItemStack itemStack;

    public WrappedPlayInBookEdit(PacketDataSerializer buf, PacketContainer<WrappedPlayInBookEdit> packet) {
        super(buf, packet);

        NetProtocol protocol = packet.getProtocol();

        if (protocol.isOlderThan(NetProtocol.v1_17)) {
            this.itemStack = buf.readItemStack(protocol);
            this.signing = buf.readBoolean();
            this.interactionHand = InteractionHand.values()[buf.readVarInt()];
        } else {
            this.interactionHand = InteractionHand.values()[buf.readVarInt()];

            int pageCount = buf.readVarInt();
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_14) && pageCount > MAX_PAGES
                    || protocol.isOlderThan(NetProtocol.v1_14) && pageCount > MAX_PAGES_1_13_2) {
                throw new IllegalStateException("Too many pages: " + pageCount + ", limit: " + (protocol.isNewerThanOrEqual(NetProtocol.v1_14) ? MAX_PAGES : MAX_PAGES_1_13_2));
            }

            CompoundTag tag = new CompoundTag();
            ArrayTag<String> pages = new ArrayTag<>(TagType.STRING);
            for (int i = 0; i < pageCount; i++) {
                pages.add(new PrimitiveTag<>(TagType.STRING, buf.readString(PAGE_CHAR_LIMIT)));
            }
            tag.put("pages", pages);

            Material material;
            if (buf.readBoolean()) {
                tag.put("title", new PrimitiveTag<>(TagType.STRING, buf.readString(TITLE_CHAR_LIMIT)));
                tag.put("author", new PrimitiveTag<>(TagType.STRING, packet.getPlayer().name));
                material = Material.WRITTEN_BOOK;
                this.signing = true;
            } else {
                material = MaterialMappings.getTypeOnly("writable-book");
                this.signing = false;
            }

            this.itemStack = WrappedImmutableItemStack.fromBukkit(new ItemStack(material));
            this.itemStack.setNBT(tag);
        }

        CodecType.BOOK_EDIT_ITEM_STACK.validate(this.itemStack, new DecodeContext.BookEditContext<>(this, this.signing));
    }

    public InteractionHand getInteractionHand() {
        return this.interactionHand;
    }

    public boolean isSigning() {
        return this.signing;
    }

    public WrappedImmutableItemStack getItemStack() {
        return this.itemStack;
    }
}
