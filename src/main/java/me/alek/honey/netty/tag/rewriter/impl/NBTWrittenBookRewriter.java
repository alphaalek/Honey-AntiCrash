package me.alek.honey.netty.tag.rewriter.impl;

import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.codec.validation.CodecContext;
import me.alek.honey.netty.tag.ComponentHelper;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.PrimitiveArrayTag;
import me.alek.honey.netty.tag.rewriter.InTagRewriter;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;

public class NBTWrittenBookRewriter implements InTagRewriter.NBTRewriter {

    public static void checkFormatted(Component component) {
        String formatted = AdventureSerializer.asVanilla(component);
        if (formatted.length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_FORMATTED_PAGE_LENGTH) {
            throw new DecoderException("Invalid book data: too long formatted page: " + formatted.length());
        }
    }

    public static Rewritable.Result<TextComponent> readRewritablePage(Component page, TagRewritingContext<?> context) {
        // we don't have to check for the formatted limit if the player is editing a book, because in
        // the book edit tag visitor we have already made sure that the page string is read by the same limit
        if (context.codec() instanceof CodecContext.StackContextImpl.BookEditing) {
            // make sure there are no other components than text content because that
            // is not possible to do from a client when signing a book
            if (page instanceof TextComponent) {
                TextComponent text = (TextComponent) page;
                if (!ComponentHelper.isRawText(text)) {
                    return Rewritable.Result.rewritten(Component.text(text.content()));
                } else {
                    return Rewritable.Result.noRewritten();
                }
            } else {
                throw new DecoderException("Invalid book data: tried to read page that is not text component");
            }
        }

        TextComponent read = ComponentHelper.readText(page);
        checkFormatted(read);
        if (page == read) {
            // if they are equal (as instances), the page has not been rewritten
            return Rewritable.Result.noRewritten();
        } else {
            return Rewritable.Result.rewritten(read);
        }
    }

    public Rewritable.Result<TextComponent> readRewritablePage(String page, TagRewritingContext<CompoundTag> context) {
        return readRewritablePage(ComponentHelper.deserialize(page), context);
    }

    public static String serializePage(TextComponent text, TagRewritingContext<CompoundTag> context) {
        if (context.codec() instanceof CodecContext.StackContextImpl.BookEditing) {
            return ComponentHelper.ensureOnlyDeserializableAsRawText(text.content());
        } else {
            return ComponentHelper.serialize(text);
        }
    }

    @Override
    public Rewritable.Result<CompoundTag> rewrite(NetProtocol protocol, TagRewritingContext<CompoundTag> context) {
        CompoundTag tag = context.tag();
        PrimitiveArrayTag<String> pages = new PrimitiveArrayTag<>(TagType.STRING);

        Bukkit.getLogger().info("Tag " + tag);

        int normalpages = 0;
        for (String page : tag.<String>getPrimitiveArrayOf("pages", TagType.STRING)) {
            Rewritable.Result<TextComponent> rewritable = readRewritablePage(page, context);
            if (rewritable.isRewritten()) {
                pages.add(serializePage(rewritable.get(), context));
            } else {
                normalpages++;
                pages.add(page);
            }
        }

        // check if any of the pages have been rewritten
        if (normalpages < pages.size()) {
            tag.put("pages", pages);
            return Rewritable.Result.rewritten(tag);
        } else {
            return Rewritable.Result.noRewritten();
        }
    }
}
