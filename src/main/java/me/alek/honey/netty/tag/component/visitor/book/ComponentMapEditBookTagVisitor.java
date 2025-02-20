package me.alek.honey.netty.tag.component.visitor.book;

import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Filterable;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.tag.dynamic.EditBookTagVisitor;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.List;

public class ComponentMapEditBookTagVisitor extends ComponentMapBookTagVisitor implements EditBookTagVisitor {

    private final String signingUsername;
    private final boolean signing;

    public ComponentMapEditBookTagVisitor(NetProtocol protocol, NetProtocol protocolCreated, String signingUsername, boolean signing) {
        super(protocol, protocolCreated, signing ? ItemTypes.WRITTEN_BOOK : ItemTypes.WRITABLE_BOOK);
        this.signingUsername = signingUsername;
        this.signing = signing;

        // Well, this implementation will never be used, because book content in edit context will never be represented as a component map
    }

    @Override
    public String signingUsername() {
        return this.signingUsername;
    }

    @Override
    public boolean signing() {
        return this.signing;
    }

    @Override
    protected String visitAtAuthor(PacketDataSerializer serializer) {
        String author = super.visitAtAuthor(serializer);
        if (!author.equals(this.signingUsername)) {
            throw new DecoderException("Invalid book data: invalid author tag: was \"" + author + "\" but should be \"" + this.signingUsername + "\"");
        }
        return author;
    }

    @Override
    protected Component visitPage(Component component) {
        // we don't have to check for the formatted limit if the player is editing a book, because in
        // the book edit tag visitor we have already made sure that the page string is read by the same limit
        if (component instanceof TextComponent) {
            // make sure there are no other components than text content because that
            // is not possible to do from a client when signing a book
            TextComponent text = (TextComponent) component;
            if (!text.children().isEmpty() || !text.style().isEmpty()) {
                return Component.text(text.content());
            } else {
                return component;
            }
        } else {
            throw new DecoderException("Invalid book data: tried to read page that is not text component");
        }
    }

    @Override
    protected AbstractTag<?, ?> visitAtSerializedPageTag(PacketDataSerializer serializer) {
        AbstractTag<?, ?> tag = super.visitAtSerializedPageTag(serializer);
        if (tag.getType() != TagType.STRING) {
            throw new DecoderException("Invalid book data: expected string tag type, but got " + tag.getType());
        }
        return tag;
    }

    @Override
    protected List<Filterable<Component>> visitAtSerializedPages(PacketDataSerializer serializer, int maxPageLength) {
        // although it's reading serialized content because it's a written book, we will not allow it, because it comes
        // from the client editing the book, and we will not allow formatting whatever components
        return super.visitAtSerializedPages(serializer, ConfigurationOptions.ItemAnalysis.BOOK_MAX_FORMATTED_PAGE_LENGTH);
    }

    @Override
    protected List<Filterable<String>> visitAtFormattedPages(PacketDataSerializer serializer, int maxPageLength) {
        // always read using the formatted text limit, because we will not allow formatting whatever components by
        // the client when editing a book
        return super.visitAtFormattedPages(serializer, ConfigurationOptions.ItemAnalysis.BOOK_MAX_FORMATTED_PAGE_LENGTH);
    }
}
