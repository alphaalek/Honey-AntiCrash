package me.alek.honey.netty.tag;

import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.tag.dynamic.BookTagVisitorFactory;
import me.alek.honey.netty.tag.nbt.BookTag;
import me.alek.honey.netty.tag.rewriter.InTagRewriter;

public class Visitors {

    public static void load() {
        BookTag.loadValidations();

        InTagRewriter.loadRegistry();

        CodecVisitor.Dynamic.IN_REGISTRY.put(ItemTypes.WRITTEN_BOOK, BookTagVisitorFactory.FACTORY);
        CodecVisitor.Dynamic.IN_REGISTRY.put(ItemTypes.WRITABLE_BOOK, BookTagVisitorFactory.FACTORY);
    }
}
