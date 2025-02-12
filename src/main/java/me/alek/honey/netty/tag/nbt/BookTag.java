package me.alek.honey.netty.tag.nbt;

import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.HoneyPacketCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.codec.validation.CodecContext;
import me.alek.honey.netty.codec.validation.CodecTypeValidation;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacketImpl;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.dynamic.EditBookTagVisitorFactory;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.netty.tag.rewriter.InTagRewriter;

import java.util.Optional;

public class BookTag {

    public static final String PAGES = "pages";
    public static final String FILTERED_PAGES = "filtered_pages";
    public static final String TITLE = "title";
    public static final String FILTERED_TITLE = "filtered_title";
    public static final String AUTHOR = "author";

    public static void loadValidations() {
        CodecTypeValidation.BOOK_EDIT_TAG.addValidation(BookTag::validateBookEdit);
        CodecTypeValidation.BOOK_EDIT_ITEM_STACK.addValidation(BookTag::validateBookEdit);
    }

    public static WrappedImmutableItemStack decodeBookEditStack(ByteInWrappedPacketImpl.CacheRewriteCaller<?> source, PacketContainer<?> packet, boolean signing, PacketDataSerializer serializer) {
        // read the item stack
        EditBookTagVisitorFactory visitorFactory = new EditBookTagVisitorFactory(false, packet.getPlayerName(), signing);
        CodecContext.StackContextImpl.BookEditing bookEditContext = CodecContext.StackContextImpl.BookEditing.from(serializer, packet.getProtocol(), packet.getProtocolCreated(), 0, packet.getPlayerName(), signing);
        int idx = serializer.readerIndex();
        // no need to read safely, because we applied the edit book tag visitor
        Rewritable<WrappedImmutableItemStack> rewritable = serializer.readItemStack(visitorFactory, bookEditContext);
        WrappedImmutableItemStack stack = rewritable.get();

        // check if there are any bytes left
        HoneyPacketCodec.checkReadable(packet.getProtocolCreated(), packet.getType(), Optional.ofNullable(packet.getObjectWrapped()).map(WrappedObject::getHandle).orElse(null), serializer);

        // validate this stack
        CodecTypeValidation.BOOK_EDIT_ITEM_STACK.validate(stack, CodecContext.BookValidationContext.from(serializer, packet, signing));

        // in some versions the client sends a writable_book when signing books instead of written_book, but the server will
        // still serialize the pages and treat it as a written_book, so we have to make sure it has been rewritten, otherwise try again
        if (!rewritable.rewritten().isRewritten() && signing && stack.getType().equals(ItemTypes.WRITABLE_BOOK)) {
            CompoundTag tag = stack.getTag().asNBT().get();
            Rewritable.Result<CompoundTag> result = InTagRewriter.NBTRewriter.REGISTRY.get(ItemTypes.WRITTEN_BOOK).rewrite(packet.getProtocol(), InTagRewriter.TagRewritingContext.of(tag, bookEditContext));
            if (result.isRewritten()) {
                // slice the serializer we will rewrite to, and mark the writer index at the end
                PacketDataSerializer rewritten = new PacketDataSerializer();
                rewritten.writeBytes(serializer.slice(0, idx));

                // write the rewritten item (tag) to the rewritten buffer
                stack.setTag(tag);
                rewritten.writeItemStack(CodecContext.from(rewritten, packet.getProtocol(), packet.getProtocolCreated()), stack);
                rewritten.readerIndex(rewritten.writerIndex());

                Rewritable<CompoundTag> tagRewritable = Rewritable.rewritten(tag, Rewritable.Result.rewritten(rewritten));
                source.handleRewritten(tagRewritable);
            }
        } else {
            source.handleRewritten(rewritable);
        }

        return stack;
    }

    public static void validateBook(WrappedImmutableItemStack item, CodecContext context) {
        ItemTag tag = item.getTag();
        if (tag.asNBT().isPresent()) {
            CompoundTag nbt = tag.asNBT().get();
            if (nbt.hasKeyOfType(PAGES, ArrayTag.TYPE.get())) {
                ArrayTag<String> pages = nbt.getArrayOf(PAGES, TagType.STRING);
                int max = ConfigurationOptions.ItemAnalysis.NBT.getMaxBookPages(context.protocolCreated());
                if (pages.size() > max) {
                    throw new DecoderException("Too many pages: " + pages.size() + ", limit: " + max);
                }
            }

            if (nbt.hasKeyOfType(TITLE, TagType.STRING)
                    && (nbt.getString(TITLE).isEmpty() || nbt.getString(TITLE).length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_TITLE_LENGTH)) {
                throw new DecoderException("Invalid book data! Too long title: " + nbt.getString(TITLE).length() + ", limit: " + ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_TITLE_LENGTH);
            }

            if (nbt.hasKeyOfType(AUTHOR, TagType.STRING)
                    && (nbt.getString(AUTHOR).isEmpty() || nbt.getString(AUTHOR).length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_AUTHOR_LENGTH)) {
                throw new DecoderException("Invalid book data! Too long author: " + nbt.getString(AUTHOR).length() + ", limit: " + ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_AUTHOR_LENGTH);
            }
        }
    }

    public static void validateBookEdit(WrappedImmutableItemStack item, CodecContext.BookValidationContext<?> context) {
        if (item == null) {
            throw new DecoderException("Invalid book edit: invalid item");
        }

        if (item.getType() != ItemTypes.WRITTEN_BOOK && item.getType() != ItemTypes.WRITABLE_BOOK) {
            throw new DecoderException("Invalid book edit item-stack! Item-stack type: " + item.getType());
        }

        ItemTag tag = item.getTag();
        if (tag.isEmpty()) {
            throw new DecoderException("Invalid book data: no tag");
        }
    }

    public static void validateBookEdit(CompoundTag nbt, CodecContext.BookValidationContext<?> context) {
        if (!nbt.hasKeyOfType(PAGES, ArrayTag.TYPE.get())) {
            throw new DecoderException("Invalid book data: no pages tag");
        }

        if (context.signing()) {
            if (!nbt.hasKeyOfType(TITLE, TagType.STRING)) {
                throw new DecoderException("Invalid book data: no title tag in sign context");
            }

            if (!nbt.hasKeyOfType(AUTHOR, TagType.STRING) || !nbt.getString(AUTHOR).equals(context.packet().getPlayerName())) {
                throw new DecoderException("Invalid book data: no valid author tag in sign context");
            }

            if (nbt.getKeys().stream().anyMatch(k ->
                    !k.equals(PAGES)
                            && !k.equals(FILTERED_PAGES)
                            && !k.equals(TITLE)
                            && !k.equals(FILTERED_TITLE)
                            && !k.equals(AUTHOR))) {
                throw new DecoderException("Invalid book data: extra tags");
            }
        } else if (nbt.getKeys().stream().anyMatch(k ->
                !k.equals(PAGES) && !k.equals(FILTERED_PAGES))) {
            throw new DecoderException("Invalid book data: extra tags");
        }
    }
}
