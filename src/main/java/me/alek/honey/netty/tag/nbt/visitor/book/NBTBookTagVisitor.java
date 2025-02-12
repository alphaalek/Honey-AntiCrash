package me.alek.honey.netty.tag.nbt.visitor.book;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.tag.FallbackDefaultCodecException;
import me.alek.honey.netty.tag.nbt.*;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.netty.tag.nbt.visitor.ComplexTagVisitor;
import me.alek.honey.util.protocol.NetProtocol;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

public class NBTBookTagVisitor extends ComplexTagVisitor {

    private final ItemType type;
    protected int pages;

    public NBTBookTagVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
        super(CodecVisitor.of(protocol, protocolCreated, type));
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
                    || key.equals(BookTag.PAGES) && !type.equals(ArrayTag.TYPE.get())
                    || key.equals(BookTag.FILTERED_PAGES) && !type.equals(CompoundTag.TYPE.get())) {
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

    @Override
    public <TAG extends AbstractTag<?, ?, TAG>> TAG visitAtArrayInsert(DataInputStream stream, TagLimiter limiter, TagType type) throws FallbackDefaultCodecException, IOException {
        if (currentGrandChild().equals(BookTag.PAGES)) {
            if (++this.pages > ConfigurationOptions.ItemAnalysis.NBT.getMaxBookPages(protocolCreated())) {
                throw new DecoderException("Invalid book data: too many pages: " + this.pages);
            }

            if (!type.equals(TagType.STRING)) {
                throw new DecoderException("Invalid book data: invalid page tag type: " + type);
            }

            return visitPage(stream, limiter, type);
        }

        return super.visitAtArrayInsert(stream, limiter, type);
    }

    @SuppressWarnings("unchecked")
    protected <TAG extends AbstractTag<?, ?, TAG>> TAG visitPage(DataInputStream stream, TagLimiter limiter, TagType type) throws IOException {
        if (this.type.equals(ItemTypes.WRITTEN_BOOK)) {
            return (TAG) type.<PrimitiveTag<String>>getCodec().read(stream, new TagLimiter.UTF8Limiter(limiter, ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGE_LENGTH), this);
        } else {
            return (TAG) type.<PrimitiveTag<String>>getCodec().read(stream, new TagLimiter.UTF8Limiter(limiter, ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_FORMATTED_PAGE_LENGTH), this);
        }
    }
}
