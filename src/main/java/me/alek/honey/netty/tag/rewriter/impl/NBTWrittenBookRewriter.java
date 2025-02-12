package me.alek.honey.netty.tag.rewriter.impl;

import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.codec.validation.CodecContext;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.netty.tag.rewriter.InTagRewriter;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class NBTWrittenBookRewriter implements InTagRewriter.NBTRewriter {

    private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.colorDownsamplingGson();

    public static void checkFormatted(Component component) {
        String formatted = AdventureSerializer.asVanilla(component);
        if (formatted.length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_FORMATTED_PAGE_LENGTH) {
            throw new DecoderException("Invalid book data: too long formatted page: " + formatted.length());
        }
    }

    public static Component readPage(Component component) {
        if (!(component instanceof TextComponent)) {
            // wrap a text component around the json text of the delegated component
            return wrapAsText(component);
        } else {
            TextComponent text = (TextComponent) component;

            List<Component> children = new ArrayList<>();
            for (Component child : text.children()) {
                if (!(child instanceof TextComponent)) {
                    // this child is not a text component ...
                    // wrap a text component around the json text of the child
                    children.add(wrapAsText(child));
                } else {
                    TextComponent childText = (TextComponent) child;
                    if (childText.content().isEmpty()) {
                        throw new DecoderException("Invalid book data: empty child component");
                    } else if (!child.children().isEmpty()) {
                        throw new DecoderException("Invalid book data: grandchildren components");
                    } else {
                        children.add(child);
                    }
                }
            }
            // check if any of the children has been modified
            if (!children.containsAll(text.children())) {
                // apply the modified children
                return text.children(children);
            } else {
                return text;
            }
        }
    }

    public static Component deserializePage(String page) {
        return GSON_COMPONENT_SERIALIZER.deserialize(page);
    }

    public Rewritable.Result<Component> readRewritablePage(String page, TagRewritingContext<CompoundTag> context) {
        Component component = deserializePage(page);
        // we don't have to check for the formatted limit if the player is editing a book, because in
        // the book edit tag visitor we have already made sure that the page string is read by the same limit
        if (context.codecContext() instanceof CodecContext.StackContextImpl.BookEditing) {
            // make sure there are no other components than text content because that
            // is not possible to do from a client when signing a book
            if (component instanceof TextComponent) {
                TextComponent text = (TextComponent) component;
                if (!text.children().isEmpty() || !text.style().isEmpty()) {
                    return Rewritable.Result.rewritten(Component.text(text.content()));
                } else {
                    return Rewritable.Result.noRewritten();
                }
            } else {
                throw new DecoderException("Invalid book data: tried to read page that is not text component");
            }
        }

        Component read = readPage(component);
        checkFormatted(read);
        if (component == read) {
            // if they are equal, the page has not been rewritten
            return Rewritable.Result.noRewritten();
        } else {
            return Rewritable.Result.rewritten(read);
        }
    }

    private static Component wrapAsText(Component component) {
        return Component.text(GSON_COMPONENT_SERIALIZER.serialize(component));
    }

    @Override
    public Rewritable.Result<CompoundTag> rewrite(NetProtocol protocol, TagRewritingContext<CompoundTag> context) {
        CompoundTag tag = context.tag();
        ArrayTag<String> pages = new ArrayTag<>(TagType.STRING);

        int normalpages = 0;
        for (String page : tag.<String>getArrayOf("pages", TagType.STRING)) {
            Rewritable.Result<Component> rewritable = readRewritablePage(page, context);
            if (rewritable.isRewritten()) {
                pages.add(GSON_COMPONENT_SERIALIZER.serialize(rewritable.get()));
            } else {
                normalpages++;
                pages.add(page);
            }
        }

        // check if any of the pages have been rewritten
        if (normalpages < pages.size()) {
            tag.put("pages", pages);
            return Rewritable.Result.rewritten(tag);
        }

        return Rewritable.Result.noRewritten();
    }
}
