package me.alek.honey.netty.tag.nbt.visitor.book;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.tag.nbt.*;
import me.alek.honey.netty.tag.nbt.visitor.ComplexTagVisitor;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

public class BookTagVisitor extends ComplexTagVisitor {

    public static class Factory implements CodecVisitor.Factory<BookTagVisitor> {

        public static final Factory FACTORY = new Factory();

        @Override
        public BookTagVisitor create(NetProtocol protocol, ItemType type) {
            return new BookTagVisitor(protocol, type);
        }
    }

    private final ItemType type;
    protected int pages;

    public BookTagVisitor(NetProtocol protocol, ItemType type) {
        super(CodecVisitor.of(protocol, false));
        this.type = type;
    }

    public boolean isRootTag() {
        return currentGrandChild().equals(NBTFactory.NAMED_COMPOUND_TAG_NAME);
    }

    @Override
    public void visitCompound(Map<String, AbstractTag<?, ?, ?>> compound) {
        if (isRootTag()) {
            if (!compound.containsKey(BookTag.PAGES)) {
                throw new DecoderException("Invalid book data: no pages tag");
            }

            if (this.type.equals(ItemTypes.WRITTEN_BOOK) && (!compound.containsKey(BookTag.AUTHOR) || !compound.containsKey(BookTag.TITLE))) {
                throw new DecoderException("Invalid book data: missing tags");
            }
        }

        super.visitCompound(compound);
    }

    @Override
    public String visitCompoundKey(String key, TagType type) {
        if (isRootTag()) {
            if ((key.equals(BookTag.AUTHOR) || key.equals(BookTag.TITLE) || key.equals(BookTag.FILTERED_TITLE)) && !type.equals(TagType.STRING)
                    || key.equals(BookTag.PAGES) && !type.equals(TagTypeCollection.getCollection(TagTypeCollection.ARRAY))
                    || key.equals(BookTag.FILTERED_PAGES) && !type.equals(TagTypeCollection.getCollection(TagTypeCollection.COMPOUND))) {
                throw new DecoderException("Invalid book data: invalid tag type: " + key + ", " + type);
            }
        }

        return super.visitCompoundKey(key, type);
    }

    @Override
    public AbstractTag<?, ?, ?> visitAtCompoundInsertTag(DataInputStream stream, TagLimiter limiter, TagType type, String key) throws FallbackDefaultCodecException, IOException {
        if (isRootTag() && this.type.equals(ItemTypes.WRITTEN_BOOK)) {
            if (key.equals(BookTag.AUTHOR)) {
                return type.<PrimitiveTag<String>>getCodec().read(stream, new TagLimiter.UTF8Limiter(limiter, ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_AUTHOR_LENGTH), this);
            } else if (key.equals(BookTag.TITLE)) {
                return type.<PrimitiveTag<String>>getCodec().read(stream, new TagLimiter.UTF8Limiter(limiter, ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_TITLE_LENGTH), this);
            }
        }

        return super.visitAtCompoundInsertTag(stream, limiter, type, key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <TAG extends AbstractTag<?, ?, TAG>> TAG visitAtArrayInsert(DataInputStream stream, TagLimiter limiter, TagType type) throws FallbackDefaultCodecException, IOException {
        if (currentGrandChild().equals(BookTag.PAGES)) {
            if (++this.pages > ConfigurationOptions.ItemAnalysis.NBT.getMaxBookPages(protocol())) {
                throw new DecoderException("Invalid book data: too many pages: " + this.pages);
            }

            if (!type.equals(TagType.STRING)) {
                throw new DecoderException("Invalid book data: invalid page tag type: " + type);
            }

            return (TAG) type.<PrimitiveTag<String>>getCodec().read(stream, new TagLimiter.UTF8Limiter(limiter, ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGE_LENGTH), this);
        }

        return super.visitAtArrayInsert(stream, limiter, type);
    }
}
