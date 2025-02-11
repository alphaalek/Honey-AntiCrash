package me.alek.honey.netty.tag.dynamic;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.tag.component.visitor.book.ComponentBookTagVisitor;
import me.alek.honey.netty.tag.nbt.visitor.book.NBTBookTagVisitor;
import me.alek.honey.util.protocol.NetProtocol;

public class BookTagVisitorFactory implements CodecVisitor.Dynamic<NBTBookTagVisitor, ComponentBookTagVisitor> {

    public static final BookTagVisitorFactory FACTORY = new BookTagVisitorFactory(false);

    private final boolean replaceByRegistered;

    public BookTagVisitorFactory(boolean replaceByRegistered) {
        this.replaceByRegistered = replaceByRegistered;
    }

    @Override
    public NBTBookTagVisitor createNBTVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
        return new NBTBookTagVisitor(protocol, protocolCreated, type);
    }

    @Override
    public ComponentBookTagVisitor createComponentVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
        return new ComponentBookTagVisitor(protocol, protocolCreated, type);
    }

    @Override
    public boolean replaceByRegistered() {
        return this.replaceByRegistered;
    }
}
