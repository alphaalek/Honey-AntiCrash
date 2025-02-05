package me.alek.honey.netty.packetwrappers.impl.play.client;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.codec.HoneyPacketCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.VarInt;
import me.alek.honey.netty.codec.validation.CodecTypeValidation;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.netty.tag.nbt.BookTag;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.EditBookTagVisitor;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

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

        if (protocol.isOlderThanOrEqual(NetProtocol.v1_17)) {
            // we want to swap the field reading order of the packet, so we can find out if this book edit packet wants
            // to sign a book or not before trying to decode the item stack, so we can apply the correct tag visitor
            // however, reversing the byte buffer gives some problems NORMALLY, but luckily not for this packet:
            //
            // the protocol notes the slot field is a VarInt, however it can just be treated as a byte
            // this is very important, because this makes it possible to reverse the byte buf correctly
            // if the packet had properties that could potentially require more space than one byte, it means the
            // correct start/end position of that section can not be identified without reading from the start
            // of the initial buffer, which we do not want, as written above.
            // Note, a boolean only requires a byte, therefore the slot and signing property only requires 1 byte each and
            // their end/start position is therefore known as they are the same
            this.slot = buf.getUnsignedByte(buf.readableBytes()); // encoded as last field, so read the last byte of this buffer
            // check if we can continue with our assumption
            if ((this.slot & VarInt.CONTINUE_BIT) != 0) {
                // Well, this client was just broken...
                throw new DecoderException("Invalid slot");
            }
            this.signing = buf.getByte(buf.readableBytes() - 1) != 0;
            PacketDataSerializer itemBuf = new PacketDataSerializer(buf.readBytes(buf.readableBytes() - 2));

            EditBookTagVisitor visitor = this.signing ? EditBookTagVisitor.of(protocol, true, packet.getPlayerName()) : EditBookTagVisitor.of(protocol);
            WrappedImmutableItemStack stack = itemBuf.readItemStackSafely(visitor, packet);
            CodecTypeValidation.BOOK_EDIT_ITEM_STACK.validate(stack, DecodeContext.BookEditContext.of(packet, this.signing));

            HoneyPacketCodec.checkReadable(protocol, PacketTypes.Play.Client.BOOK_EDIT, null, itemBuf);

            // will always represent nbt as we are before or equal 1.17
            CompoundTag tag = stack.getTag().asNBT().get();
            for (String page : tag.<String>getArrayOf(BookTag.PAGES, TagType.STRING)) {
                this.pages.add(page);
            }
            if (this.signing) {
                this.author = tag.getString(BookTag.AUTHOR);
                this.title = tag.getString(BookTag.TITLE);
            }

            // rinse
            buf.readBytes(2);
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

            this.signing = buf.readBoolean();
            if (this.signing) {
                this.title = buf.readString(TITLE_CHAR_LIMIT);
                this.author = packet.getPlayer().name;
            }

            validateModernBookEdit(protocol, packet);
        }

        if (this.slot < 0 || this.slot > 8) {
            throw new DecoderException("Invalid slot");
        }
    }

    private void validateModernBookEdit(NetProtocol protocol, PacketContainer<WrappedPlayInBookEdit> packet) {
        if (protocol.isOlderThan(NetProtocol.v1_20_5)) {
            CompoundTag tag = new CompoundTag();
            tag.putArray("pages", TagType.STRING, this.pages);

            if (this.signing) {
                tag.putPrimitive("title", TagType.STRING, this.title);
                tag.putPrimitive("author", TagType.STRING, this.author);
            }

            CodecTypeValidation.BOOK_EDIT_TAG.validate(tag, DecodeContext.BookEditContext.of(packet, this.signing));
        }
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
