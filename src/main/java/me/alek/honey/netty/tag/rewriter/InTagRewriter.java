package me.alek.honey.netty.tag.rewriter;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.codec.validation.CodecContext;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.*;

public interface InTagRewriter<TAG extends ItemTag> {

    interface TagRewritingContext<TAG extends ItemTag> {

        static <TAG extends ItemTag> TagRewritingContext<TAG> of(TAG tag, CodecContext context) {
            return new TagRewritingContext<TAG>() {
                @Override
                public TAG tag() {
                    return tag;
                }

                @Override
                public CodecContext codecContext() {
                    return context;
                }
            };
        }

        TAG tag();

        CodecContext codecContext();
    }

    interface NBTRewriter extends InTagRewriter<CompoundTag> {

        Map<ItemType, NBTRewriter> REGISTRY = new HashMap<>();

        static void loadRegistry() {
            REGISTRY.put(ItemTypes.WRITTEN_BOOK, new InTagRewriter.NBTRewriter() {

                private final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.colorDownsamplingGson();

                @Override
                public Rewritable.Result<CompoundTag> rewrite(NetProtocol protocol, TagRewritingContext<CompoundTag> context) {
                    CompoundTag tag = context.tag();
                    ArrayTag<String> pages = new ArrayTag<>(TagType.STRING);

                    int normalpages = 0;
                    for (String page : tag.<String>getArrayOf("pages", TagType.STRING)) {
                        Component component = GSON_COMPONENT_SERIALIZER.deserialize(page);
                        if (!(component instanceof TextComponent)) {
                            // wrap a text component around the json text of the delegated component
                            pages.add(GSON_COMPONENT_SERIALIZER.serialize(toText(component)));
                            continue;
                        } else {
                            TextComponent text = (TextComponent) component;
                            // we don't have to check for the formatted limit if the player is editing a book, because in
                            // the book edit tag visitor we have already made sure that the page string is read by the same limit
                            if (context.codecContext() instanceof CodecContext.StackContextImpl.BookEditing) {
                                // make sure there are no other components than text content because that
                                // is not possible to do from a client when signing a book
                                if (!text.children().isEmpty() || !text.style().isEmpty()) {
                                    pages.add(GSON_COMPONENT_SERIALIZER.serialize(Component.text(text.content())));
                                    continue;
                                }
                            } else {
                                List<Component> children = new ArrayList<>();
                                for (Component child : text.children()) {
                                    if (!(child instanceof TextComponent)) {
                                        // this child is not a text component ...
                                        // wrap a text component around the json text of the child
                                        children.add(toText(child));
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
                                // check the formatted length after we have modified the children
                                String formatted = AdventureSerializer.asVanilla(text.children(children));
                                if (formatted.length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_FORMATTED_PAGE_LENGTH) {
                                    throw new DecoderException("Invalid book data: too long formatted page: " + formatted.length() + " > " + ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_FORMATTED_PAGE_LENGTH);
                                }
                                // check if any of the children has been modified
                                if (!children.containsAll(text.children())) {
                                    // apply the modified children
                                    pages.add(GSON_COMPONENT_SERIALIZER.serialize(text.children(children)));
                                    continue;
                                }
                            }
                        }
                        // fallback to the original page
                        pages.add(page);
                        normalpages++;
                    }

                    // check if any of the pages have been rewritten
                    if (normalpages < pages.size()) {
                        tag.put("pages", pages);
                        return Rewritable.Result.rewritten(tag);
                    }

                    return Rewritable.Result.noRewritten();
                }

                private Component toText(Component component) {
                    return Component.text(GSON_COMPONENT_SERIALIZER.serialize(component));
                }
            });
        }
    }

    interface ComponentMapRewriter extends InTagRewriter<ComponentMap> {

        Map<ItemType, ComponentMapRewriter> REGISTRY = new HashMap<>();
    }

    Rewritable.Result<TAG> rewrite(NetProtocol protocol, TagRewritingContext<TAG> context);
}
