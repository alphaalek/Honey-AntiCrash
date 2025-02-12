package me.alek.honey.netty.tag.rewriter;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.codec.validation.CodecContext;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.rewriter.impl.ComponentMapWrittenBookRewriter;
import me.alek.honey.netty.tag.rewriter.impl.NBTWrittenBookRewriter;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.HashMap;
import java.util.Map;

public interface InTagRewriter<TAG extends ItemTag> {

    interface TagRewritingContext<TAG extends ItemTag> {

        static <TAG extends ItemTag> TagRewritingContext<TAG> of(TAG tag, CodecContext context) {
            return new TagRewritingContext<TAG>() {
                @Override
                public TAG tag() {
                    return tag;
                }

                @Override
                public CodecContext codec() {
                    return context;
                }
            };
        }

        TAG tag();

        CodecContext codec();
    }

    static void loadRegistry() {
        NBTRewriter.REGISTRY.put(ItemTypes.WRITTEN_BOOK, new NBTWrittenBookRewriter());
        ComponentMapRewriter.REGISTRY.put(ItemTypes.WRITTEN_BOOK, new ComponentMapWrittenBookRewriter());
    }

    interface NBTRewriter extends InTagRewriter<CompoundTag> {

        Map<ItemType, NBTRewriter> REGISTRY = new HashMap<>();
    }

    interface ComponentMapRewriter extends InTagRewriter<ComponentMap> {

        Map<ItemType, ComponentMapRewriter> REGISTRY = new HashMap<>();
    }

    Rewritable.Result<TAG> rewrite(NetProtocol protocol, TagRewritingContext<TAG> context);
}
