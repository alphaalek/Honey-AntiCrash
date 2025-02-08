package me.alek.honey.netty.tag.nbt.visitor;

import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.tag.nbt.BookTag;
import me.alek.honey.netty.tag.nbt.visitor.book.BookTagVisitor;

public class Visitors {

    public static void load() {
        BookTag.loadValidations();

        CodecVisitor.Factory.IN_REGISTRY.put(ItemTypes.WRITTEN_BOOK, BookTagVisitor.Factory.FACTORY);
        CodecVisitor.Factory.IN_REGISTRY.put(ItemTypes.WRITABLE_BOOK, BookTagVisitor.Factory.FACTORY);
    }
}
