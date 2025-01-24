package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.validation.CodecType;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.PrimitiveTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.List;
import java.util.stream.Collectors;

public class WrappedPlayInBookEdit extends ByteInWrappedPacket<WrappedPlayInBookEdit> {

    public static final int PAGE_CHAR_LIMIT = 1024;
    public static final int TITLE_CHAR_LIMIT = 16;
    public static final int AUTHOR_CHAR_LIMIT = 16;
    public static final int MAX_PAGES = 100;
    public static final int MAX_PAGES_1_13_2 = 50;

    @Property
    private final int slot;
    @Property
    private final boolean signing;
    @Property
    private final List<String> pages;
    @Property
    @Property.IgnoreIfNull
    private String author;
    @Property
    @Property.IgnoreIfNull
    private String title;

    public WrappedPlayInBookEdit(PacketDataSerializer buf, PacketContainer<WrappedPlayInBookEdit> packet) {
        super(buf, packet);

        this.slot = buf.readVarInt();

        NetProtocol protocol = packet.getProtocol();
        int pageCount = buf.readVarInt();
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_14) && pageCount > MAX_PAGES
                || protocol.isOlderThan(NetProtocol.v1_14) && pageCount > MAX_PAGES_1_13_2) {
            throw new IllegalStateException("Too many pages: " + pageCount + ", limit: " + (protocol.isNewerThanOrEqual(NetProtocol.v1_14) ? MAX_PAGES : MAX_PAGES_1_13_2));
        }

        CompoundTag tag = new CompoundTag();
        ArrayTag<String> pages = new ArrayTag<>(TagType.STRING);
        for (int i = 0; i < pageCount; i++) {
            pages.add(buf.readString(PAGE_CHAR_LIMIT));
        }
        tag.put("pages", pages);
        this.pages = pages.stream().collect(Collectors.toList());

        if (buf.readBoolean()) {
            tag.put("title", new PrimitiveTag<>(TagType.STRING, this.title = buf.readString(TITLE_CHAR_LIMIT)));
            tag.put("author", new PrimitiveTag<>(TagType.STRING, this.author = packet.getPlayer().name));
            this.signing = true;
        } else {
            this.signing = false;
        }

        CodecType.BOOK_EDIT_TAG.validate(tag, new DecodeContext.BookEditContext<>(this, this.signing));
    }

    public int getSlot() {
        return this.slot;
    }

    public boolean isSigning() {
        return this.signing;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getTitle() {
        return this.title;
    }

    public List<String> getPages() {
        return this.pages;
    }
}
