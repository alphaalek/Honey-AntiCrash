package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.validation.CodecTypeValidation;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.PrimitiveTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.util.ArrayList;
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
    private final List<String> pages = new ArrayList<>();
    @Property
    @Property.IgnoreIfNull
    private String author;
    @Property
    @Property.IgnoreIfNull
    private String title;

    public WrappedPlayInBookEdit(PacketDataSerializer buf, PacketContainer<WrappedPlayInBookEdit> packet) {
        super(buf, packet);
        NetProtocol protocol = packet.getProtocol();

        CompoundTag tag;
        if (protocol.isOlderThanOrEqual(NetProtocol.v1_17)) {
            WrappedImmutableItemStack stack = buf.readItemStackSafely(protocol, this);
            tag = stack.getTag().asNBT().get();

            for (String page : tag.<String>getArrayOf("pages", TagType.STRING)) {
                this.pages.add(page);
            }
            this.signing = buf.readBoolean();
            if (this.signing) {
                this.author = tag.getString("author");
                this.title = tag.getString("title");
            }

            this.slot = buf.readVarInt();
        } else {
            this.slot = buf.readVarInt();

            int pageCount = buf.readVarInt();
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_14) && pageCount > MAX_PAGES
                    || protocol.isOlderThan(NetProtocol.v1_14) && pageCount > MAX_PAGES_1_13_2) {
                throw new IllegalStateException("Too many pages: " + pageCount + ", limit: " + (protocol.isNewerThanOrEqual(NetProtocol.v1_14) ? MAX_PAGES : MAX_PAGES_1_13_2));
            }
            for (int i = 0; i < pageCount; i++) {
                this.pages.add(buf.readString(PAGE_CHAR_LIMIT));
            }
            tag = new CompoundTag();
            tag.putArray("pages", TagType.STRING, this.pages);

            if (buf.readBoolean()) {
                tag.putPrimitive("title", TagType.STRING, this.title = buf.readString(TITLE_CHAR_LIMIT));
                tag.putPrimitive("author", TagType.STRING, this.author = packet.getPlayer().name);
                this.signing = true;
            } else {
                this.signing = false;
            }
        }

        CodecTypeValidation.BOOK_EDIT_TAG.validate(tag, new DecodeContext.BookEditContext<>(this, this.signing));
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
