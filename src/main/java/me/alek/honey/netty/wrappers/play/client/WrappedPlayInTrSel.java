package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInTrSel extends WrappedPacket<WrappedPlayInTrSel> {

    public WrappedPlayInTrSel(Object rawPacket, PacketContainer<WrappedPlayInTrSel> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
