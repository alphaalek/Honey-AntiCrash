package me.alek.honey.netty.tag.dynamic;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.tag.component.visitor.book.ComponentEditBookTagVisitor;
import me.alek.honey.netty.tag.nbt.visitor.book.NBTEditBookTagVisitor;
import me.alek.honey.util.protocol.NetProtocol;

public class EditBookTagVisitorFactory implements CodecVisitor.Dynamic<NBTEditBookTagVisitor, ComponentEditBookTagVisitor> {

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
    public ComponentEditBookTagVisitor createComponentVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
        return new ComponentEditBookTagVisitor(protocol, protocolCreated, this.signingUsername, this.signing);
    }

    @Override
    public boolean replaceByRegistered() {
        return this.replaceByRegistered;
    }
}
