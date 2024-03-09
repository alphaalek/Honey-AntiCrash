package me.alek.honey.packetwrappers.play.client.common;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.packetwrappers.WrappedPacket;

public abstract class WrappedAbstractBlockInteraction<WP extends WrappedPacket<WP>> extends WrappedPacket<WP> {


    public WrappedAbstractBlockInteraction(Object rawPacket, PacketContainer<WP> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public abstract WrappedBlockPosition getBlockPosition();

}
