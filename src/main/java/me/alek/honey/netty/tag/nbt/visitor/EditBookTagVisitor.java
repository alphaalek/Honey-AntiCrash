package me.alek.honey.netty.tag.nbt.visitor;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.types.Utf8String;
import me.alek.honey.netty.tag.nbt.*;
import me.alek.honey.util.protocol.NetProtocol;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EditBookTagVisitor extends TagVisitor.In.Adapter {

    static {
        BookTag.loadValidations();
    }

    public static EditBookTagVisitor of(NetProtocol protocol, boolean signing, String signingUsername) {
        return new EditBookTagVisitor(protocol, signing, signingUsername);
    }

    public static EditBookTagVisitor of(NetProtocol protocol) {
        return of(protocol, false, null);
    }

    private final NetProtocol protocol;
    private final boolean signing;
    private final String signingUsername;
    private final List<String> keys = new ArrayList<>();

    private boolean readingFilteredPages;
    private boolean readingPages;
    private int pages;

    public EditBookTagVisitor(NetProtocol protocol, boolean signing, String signingUsername) {
        this.protocol = protocol;
        this.signing = signing;
        this.signingUsername = signingUsername;
    }

    @Override
    public NetProtocol protocol() {
        return this.protocol;
    }

    @Override
    public String visitAtCompoundInsertKey(DataInputStream stream, TagType type) throws IOException {
        String key = Utf8String.readUTFInputStream(stream, ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_KEY_LENGTH);
        if (!this.readingFilteredPages) {
            if (this.keys.contains(key)) {
                throw new DecoderException("Invalid book data: duplicate tag: " + key);
            }
            this.keys.add(key);

            if (this.signing
                    && !key.equals(BookTag.AUTHOR)
                    && !key.equals(BookTag.PAGES)
                    && !key.equals(BookTag.FILTERED_PAGES)
                    && !key.equals(BookTag.TITLE)
                    && !key.equals(BookTag.FILTERED_TITLE)) {
                throw new DecoderException("Invalid book data: extra tags");
            } else if (!this.signing
                    && !key.equals(BookTag.FILTERED_PAGES)
                    && !key.equals(BookTag.PAGES)) {
                throw new DecoderException("Invalid book data: extra tags");
            }

            if ((key.equals(BookTag.AUTHOR) || key.equals(BookTag.TITLE) || key.equals(BookTag.FILTERED_TITLE)) && !type.equals(TagType.STRING)
                    || key.equals(BookTag.PAGES) && !type.equals(TagTypeCollection.getCollection(TagTypeCollection.ARRAY))
                    || key.equals(BookTag.FILTERED_PAGES) && !type.equals(TagTypeCollection.getCollection(TagTypeCollection.COMPOUND))) {
                throw new DecoderException("Invalid book data: invalid tag type: " + key + ", " + type);
            }

            if (key.equals(BookTag.FILTERED_PAGES)) {
                this.readingFilteredPages = true;
            } else if (key.equals(BookTag.PAGES)) {
                this.readingPages = true;
            }
        }

        return key;
    }

    @Override
    public AbstractTag<?, ?, ?> visitAtCompoundInsertTag(DataInputStream stream, TagLimiter limiter, TagType type, String key) throws FallbackDefaultCodecException, IOException {
        if (this.signing) {
            if (key.equals(BookTag.AUTHOR)) {
                PrimitiveTag<String> author = type.<PrimitiveTag<String>>getCodec().read(stream, new TagLimiter.UTF8Limiter(limiter, ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_AUTHOR_LENGTH), this);
                if (!author.getValue().equals(this.signingUsername)) {
                    throw new DecoderException("Invalid book data: invalid author tag: " + author.getValue() + ", " + this.signingUsername + " (player)");
                }
                return author;
            } else if (key.equals(BookTag.TITLE)) {
                return type.<PrimitiveTag<String>>getCodec().read(stream, new TagLimiter.UTF8Limiter(limiter, ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_TITLE_LENGTH), this);
            }
        }
        if (this.readingFilteredPages) {
            if (type.getCollection() == TagTypeCollection.COMPOUND) {
                throw new DecoderException("Invalid book data: invalid filtered pages: compound within");
            }

            if (key.equals(BookTag.FILTERED_PAGES)) {
                throw new DecoderException("Invalid book data: invalid filtered pages: filtered pages within");
            }
        }

        throw new FallbackDefaultCodecException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <TAG extends AbstractTag<?, ?, TAG>> TAG visitAtArrayInsert(DataInputStream stream, TagLimiter limiter, TagType type) throws FallbackDefaultCodecException, IOException {
        if (this.readingPages) {
            if (++this.pages > ConfigurationOptions.ItemAnalysis.NBT.getMaxBookPages(this.protocol)) {
                throw new DecoderException("Invalid book data: too many pages: " + this.pages);
            }

            if (!type.equals(TagType.STRING)) {
                throw new DecoderException("Invalid book data: invalid page tag type: " + type);
            }

            return (TAG) type.<PrimitiveTag<String>>getCodec().read(stream, new TagLimiter.UTF8Limiter(limiter, ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGE_LENGTH), this);
        }

        throw new FallbackDefaultCodecException();
    }

    @Override
    public void visitCompoundElement(String key, AbstractTag<?, ?, ?> tag) {
        if (key.equals(BookTag.FILTERED_PAGES)) {
            this.readingFilteredPages = false;
        } else if (key.equals(BookTag.PAGES)) {
            this.readingPages = false;
            this.pages = 0;
        }
    }

    @Override
    public void visitCompound(Map<String, AbstractTag<?, ?, ?>> compound) {
        if (!this.readingFilteredPages) {
            if (!compound.containsKey(BookTag.PAGES)) {
                throw new DecoderException("Invalid book data: no pages tag");
            }

            if (this.signing && (!compound.containsKey(BookTag.AUTHOR) || !compound.containsKey(BookTag.TITLE))) {
                throw new DecoderException("Invalid book data: missing tags");
            }
        }
    }
}
