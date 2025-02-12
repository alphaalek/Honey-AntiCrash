package me.alek.honey.netty.tag.nbt.visitor.book;

import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.tag.dynamic.EditBookTagVisitor;
import me.alek.honey.netty.tag.nbt.*;
import me.alek.honey.util.protocol.NetProtocol;

import java.io.DataInputStream;
import java.io.IOException;

public class NBTEditBookTagVisitor extends NBTBookTagVisitor implements EditBookTagVisitor {

    private final boolean signing;
    private final String signingUsername;

    public NBTEditBookTagVisitor(NetProtocol protocol, NetProtocol protocolCreated, String signingUsername, boolean signing) {
        super(protocol, protocolCreated, signing ? ItemTypes.WRITTEN_BOOK : ItemTypes.WRITABLE_BOOK);
        this.signingUsername = signingUsername;
        this.signing = signing;
    }

    @Override
    public String visitCompoundKey(String key, TagType type) {
        if (isRootTag()) {
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
        }

        return super.visitCompoundKey(key, type);
    }

    @Override
    public void visitCompoundElement(String key, AbstractTag<?, ?, ?> tag) {
        if (isRootTag() && this.signing && key.equals(BookTag.AUTHOR)) {
            String author = tag.<String>asPrimitive().getValue();
            if (!author.equals(this.signingUsername)) {
                throw new DecoderException("Invalid book data: invalid author tag: was \"" + author + "\" but should be \"" + this.signingUsername + "\"");
            }
        }

        if (isRootTag() && key.equals(BookTag.PAGES) && tag.asArray().isEmpty()) {
            throw new DecoderException("Invalid book data: empty pages tag");
        }

        super.visitCompoundElement(key, tag);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <TAG extends AbstractTag<?, ?, TAG>> TAG visitPage(DataInputStream stream, TagLimiter limiter, TagType type) throws IOException {
        // always read using the formatted text limit, because we will not allow formatting whatever components by
        // the client when editing a book
        return (TAG) type.<PrimitiveTag<String>>getCodec().read(stream, new TagLimiter.UTF8Limiter(limiter, ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_FORMATTED_PAGE_LENGTH), this);
    }

    @Override
    public String signingUsername() {
        return this.signingUsername;
    }

    @Override
    public boolean signing() {
        return this.signing;
    }
}
