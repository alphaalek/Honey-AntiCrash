package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInTrSel extends WrappedPacket<WrappedPlayInTrSel> {

    public WrappedPlayInTrSel(Object rawPacket, PacketContainer<WrappedPlayInTrSel> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
