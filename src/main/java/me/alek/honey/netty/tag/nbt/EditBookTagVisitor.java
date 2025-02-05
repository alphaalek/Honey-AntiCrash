package me.alek.honey.netty.tag.nbt;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.io.DataInputStream;
import java.io.IOException;
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

    private boolean readingFilteredPages;

    public EditBookTagVisitor(NetProtocol protocol, boolean signing, String signingUsername) {
        this.protocol = protocol;
        this.signing = signing;
        this.signingUsername = signingUsername;

        // TODO: check filtered pages compound
    }

    @Override
    public NetProtocol protocol() {
        return this.protocol;
    }

    @Override
    public String visitAtCompoundInsertKey(DataInputStream stream, TagType type) throws IOException {
        String key = stream.readUTF();
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
        }

        return key;
    }

    @Override
    public AbstractTag<?, ?, ?> visitAtCompoundInsertTag(DataInputStream stream, TagLimiter limiter, TagType type, String key) throws FallbackDefaultCodecException, IOException {
        if (this.signing && key.equals(BookTag.AUTHOR)) {
            PrimitiveTag<String> author = type.<PrimitiveTag<String>>getCodec().read(stream, limiter, this);
            if (!author.getValue().equals(this.signingUsername)) {
                throw new DecoderException("Invalid book data: invalid author tag: " + author.getValue() + ", " + this.signingUsername + " (player)");
            }
            return author;
        }

        throw new FallbackDefaultCodecException();
    }

    @Override
    public void visitCompoundElement(String key, AbstractTag<?, ?, ?> tag) {
        if (key.equals(BookTag.FILTERED_PAGES)) {
            this.readingFilteredPages = false;
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
