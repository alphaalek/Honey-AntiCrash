package me.alek.honey.netty.tag.nbt.visitor.book;

import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.tag.nbt.*;
import me.alek.honey.util.protocol.NetProtocol;

public class EditBookTagVisitor extends BookTagVisitor {

    public static EditBookTagVisitor of(NetProtocol protocol, NetProtocol protocolCreated, boolean signing, String signingUsername) {
        return new EditBookTagVisitor(protocol, protocolCreated, signing, signingUsername);
    }

    public static EditBookTagVisitor of(NetProtocol protocol, NetProtocol protocolCreated) {
        return of(protocol, protocolCreated, false, null);
    }

    private final boolean signing;
    private final String signingUsername;

    public EditBookTagVisitor(NetProtocol protocol, NetProtocol protocolCreated, boolean signing, String signingUsername) {
        super(protocol, protocolCreated, signing ? ItemTypes.WRITTEN_BOOK : ItemTypes.WRITABLE_BOOK);
        this.signing = signing;
        this.signingUsername = signingUsername;
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
                throw new DecoderException("Invalid book data: invalid author tag: " + author + ", " + this.signingUsername + " (player)");
            }
        }

        super.visitCompoundElement(key, tag);
    }
}
