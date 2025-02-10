package me.alek.honey.netty.tag.rewriter;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.TagTypeCollection;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.HashMap;
import java.util.Map;

public interface InTagRewriter<TAG extends ItemTag> {

    interface NBTRewriter extends InTagRewriter<CompoundTag> {

        Map<ItemType, NBTRewriter> REGISTRY = new HashMap<>();

        static void loadRegistry() {
            REGISTRY.put(ItemTypes.WRITTEN_BOOK, new InTagRewriter.NBTRewriter() {

                private final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.colorDownsamplingGson();

                @Override
                public Rewritable.Result<CompoundTag> rewrite(NetProtocol protocol, CompoundTag tag) {
                    if (tag.hasKeyOfType("pages", TagTypeCollection.getFromCollection(TagTypeCollection.ARRAY))) {
                        ArrayTag<String> pages = new ArrayTag<>(TagType.STRING);
                        boolean rewritten = false;
                        for (String page : tag.<String>getArrayOf("pages", TagType.STRING)) {
                            Component component = GSON_COMPONENT_SERIALIZER.deserialize(page);
                            if (!(component instanceof TextComponent)) {
                                component = Component.text(GSON_COMPONENT_SERIALIZER.serialize(component));
                                pages.add(GSON_COMPONENT_SERIALIZER.serialize(component));
                                rewritten = true;
                            } else {
                                pages.add(page);
                            }
                        }

                        if (rewritten) {
                            tag.put("pages", pages);
                            return Rewritable.Result.rewritten(tag);
                        }
                    }

                    return Rewritable.Result.noRewritten();
                }
            });
        }
    }

    interface ComponentMapRewriter extends InTagRewriter<ComponentMap> {

        Map<ItemType, ComponentMapRewriter> REGISTRY = new HashMap<>();
    }

    Rewritable.Result<TAG> rewrite(NetProtocol protocol, TAG tag);
}
