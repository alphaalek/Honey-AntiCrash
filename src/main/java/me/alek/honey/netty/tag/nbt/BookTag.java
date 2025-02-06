package me.alek.honey.netty.tag.nbt;

import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.validation.CodecTypeValidation;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;

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

    public static void validateBook(WrappedImmutableItemStack item, DecodeContext context) {
        ItemTag tag = item.getTag();
        if (tag.asNBT().isPresent()) {
            CompoundTag nbt = tag.asNBT().get();
            if (nbt.hasKeyOfType(PAGES, TagTypeCollection.getCollection(TagTypeCollection.ARRAY))) {
                ArrayTag<String> pages = nbt.getArrayOf(PAGES, TagType.STRING);
                int max = ConfigurationOptions.ItemAnalysis.NBT.getMaxBookPages(context.protocol());
                if (pages.size() > max) {
                    throw new IllegalStateException("Too many pages: " + pages.size() + ", limit: " + max);
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

    public static void validateBookEdit(WrappedImmutableItemStack item, DecodeContext.BookEditContext<?> context) {
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

    public static void validateBookEdit(CompoundTag nbt, DecodeContext.BookEditContext<?> context) {
        if (!nbt.hasKeyOfType(PAGES, TagTypeCollection.getCollection(TagTypeCollection.ARRAY))) {
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
