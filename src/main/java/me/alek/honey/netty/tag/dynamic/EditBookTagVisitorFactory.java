package me.alek.honey.netty.tag.dynamic;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.tag.component.visitor.book.ComponentMapEditBookTagVisitor;
import me.alek.honey.netty.tag.nbt.visitor.book.NBTEditBookTagVisitor;
import me.alek.honey.util.protocol.NetProtocol;

public class EditBookTagVisitorFactory implements CodecVisitor.Dynamic<NBTEditBookTagVisitor, ComponentMapEditBookTagVisitor> {

    private final boolean replaceByRegistered;
    private final String signingUsername;
    private final boolean signing;

    public EditBookTagVisitorFactory(boolean replaceByRegistered, String signingUsername, boolean signing) {
        this.replaceByRegistered = replaceByRegistered;
        this.signingUsername = signingUsername;
        this.signing = signing;
    }

    @Override
    public NBTEditBookTagVisitor createNBTVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
        return new NBTEditBookTagVisitor(protocol, protocolCreated, this.signingUsername, this.signing);
    }

    @Override
    public ComponentMapEditBookTagVisitor createComponentMapVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
        // will never be used :-(
        return new ComponentMapEditBookTagVisitor(protocol, protocolCreated, this.signingUsername, this.signing);
    }

    @Override
    public boolean replaceByRegistered() {
        return this.replaceByRegistered;
    }
}
