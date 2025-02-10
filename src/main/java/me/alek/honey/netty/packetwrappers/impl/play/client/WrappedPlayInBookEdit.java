package me.alek.honey.netty.packetwrappers.impl.play.client;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.HoneyPacketCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.codec.types.VarInt;
import me.alek.honey.netty.codec.validation.CodecTypeValidation;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacketImpl;
import me.alek.honey.netty.packetwrappers.protocol.InteractionHand;
import me.alek.honey.netty.tag.nbt.BookTag;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.visitor.book.EditBookTagVisitor;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.ArrayList;
import java.util.List;

public class WrappedPlayInBookEdit extends ByteInWrappedPacketImpl.CacheRewriteCaller<WrappedPlayInBookEdit> {

    @Property(from = @NetProtocol.From(from = NetProtocol.v1_16_4))
    private int slot;
    @Property(from = @NetProtocol.From(from = NetProtocol.v1_13_1), to = @NetProtocol.To(to = NetProtocol.v1_16_3))
    private InteractionHand interactionHand;
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
        NetProtocol protocol = getProtocol();
        NetProtocol protocolCreated = getProtocolCreated();

        if (protocol.isOlderThanOrEqual(NetProtocol.v1_17)) {
            // we want to swap the field reading order of the packet, so we can find out if this book edit packet wants
            // to sign a book or not before trying to decode the item stack, so we can apply the correct tag visitor
            // however, reversing the byte buffer gives some problems NORMALLY, but luckily not for this packet:
            //
            // the protocol notes the interaction-hand-idx/slot field is a VarInt, however it can just be treated as a byte
            // this is very important, because this makes it possible to reverse the byte buf correctly
            // if the packet had properties that could potentially require more space than one byte and is irreversible,
            // it means the correct start/end position of that section can not be identified without reading from the start
            // of the initial buffer, which we do not want, as written above.
            // Note, a boolean (signing property) only requires a byte, therefore the interaction-hand-idx/slot field and
            // signing property field only requires 1 byte each and their end/start position is therefore known as they are the same
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_13_1)) {
                int slot = buf.getUnsignedByte(buf.readableBytes()); // encoded as last field, so read the last byte of this buffer
                // check if we can continue with our assumption
                if ((slot & VarInt.CONTINUE_BIT) != 0) {
                    // Well, this client was just broken (I guess?) ...
                    throw new DecoderException("Invalid slot");
                }

                if (protocol.isNewerThanOrEqual(NetProtocol.v1_16_4)) {
                    this.slot = slot;
                } else {
                    this.interactionHand = InteractionHand.values()[slot];
                }
            }
            this.signing = buf.getByte(buf.readableBytes() - 1) != 0;
            PacketDataSerializer itemBuf = new PacketDataSerializer(buf.readBytes(buf.readableBytes() - 2));

            WrappedImmutableItemStack stack = BookTag.decodeBookEditStack(this, packet, this.signing, itemBuf);
            itemBuf.release();

            // will always represent nbt as we are before 1.17.1
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
            int maxPageCount = ConfigurationOptions.ItemAnalysis.NBT.getMaxBookPages(getProtocol());
            if (pageCount > maxPageCount) {
                throw new IllegalStateException("Too many pages: " + pageCount + ", limit: " + maxPageCount);
            }
            for (int i = 0; i < pageCount; i++) {
                this.pages.add(buf.readString(ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGE_LENGTH));
            }

            this.signing = buf.readBoolean();
            if (this.signing) {
                this.title = buf.readString(ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_TITLE_LENGTH);
                this.author = packet.getPlayer().name;
            }

            validateModernBookEdit(protocolCreated, buf, packet);
        }

        if (this.slot < 0 || this.slot > 8) {
            throw new DecoderException("Invalid slot");
        }
    }

    private void validateModernBookEdit(NetProtocol protocol, PacketDataSerializer serializer, PacketContainer<WrappedPlayInBookEdit> packet) {
        if (protocol.isOlderThan(NetProtocol.v1_20_5)) {
            CompoundTag tag = new CompoundTag();
            tag.putArray("pages", TagType.STRING, this.pages);

            if (this.signing) {
                tag.putPrimitive("title", TagType.STRING, this.title);
                tag.putPrimitive("author", TagType.STRING, this.author);
            }

            CodecTypeValidation.BOOK_EDIT_TAG.validate(tag, DecodeContext.BookEditContext.from(serializer, packet, this.signing));
        }
    }

    public int getSlot() {
        return this.slot;
    }

    public InteractionHand getInteractionHand() {
        return this.interactionHand;
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
