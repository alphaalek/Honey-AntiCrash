package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInTrSel extends WrappedPacket<WrappedPlayInTrSel> {

    public WrappedPlayInTrSel(Object rawPacket, PacketContainer<WrappedPlayInTrSel> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
