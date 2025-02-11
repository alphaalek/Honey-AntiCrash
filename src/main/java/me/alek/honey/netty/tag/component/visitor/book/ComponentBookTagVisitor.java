package me.alek.honey.netty.tag.component.visitor.book;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import me.alek.honey.netty.tag.component.visitor.ComponentVisitor;
import me.alek.honey.util.protocol.NetProtocol;

public class ComponentBookTagVisitor extends ComponentVisitor.In.Adapter {

    private final ItemType type;
    private final NetProtocol protocol;
    private final NetProtocol protocolCreated;

    public ComponentBookTagVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
        this.protocol = protocol;
        this.protocolCreated = protocolCreated;
        this.type = type;
    }

    @Override
    public ItemType type() {
        return this.type;
    }

    @Override
    public NetProtocol protocol() {
        return this.protocol;
    }

    @Override
    public NetProtocol protocolCreated() {
        return this.protocolCreated;
    }
}
