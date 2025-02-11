package me.alek.honey.netty.tag.component.visitor.book;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import me.alek.honey.netty.tag.component.visitor.ComponentVisitor;
import me.alek.honey.netty.tag.dynamic.EditBookTagVisitor;
import me.alek.honey.util.protocol.NetProtocol;

public class ComponentEditBookTagVisitor extends ComponentVisitor.In.Adapter implements EditBookTagVisitor {

    private final NetProtocol protocol;
    private final NetProtocol protocolCreated;
    private final String signingUsername;
    private final boolean signing;

    public ComponentEditBookTagVisitor(NetProtocol protocol, NetProtocol protocolCreated, String signingUsername, boolean signing) {
        this.protocol = protocol;
        this.protocolCreated = protocolCreated;
        this.signingUsername = signingUsername;
        this.signing = signing;
    }

    @Override
    public ItemType type() {
        return this.signing ? ItemTypes.WRITTEN_BOOK : ItemTypes.WRITABLE_BOOK;
    }

    @Override
    public NetProtocol protocol() {
        return this.protocol;
    }

    @Override
    public NetProtocol protocolCreated() {
        return this.protocolCreated;
    }

    @Override
    public String signingUsername() {
        return this.signingUsername;
    }

    @Override
    public boolean signing() {
        return this.signing;
    }
}
