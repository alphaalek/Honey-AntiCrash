package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;

public class WrappedPlayInCloseWindow extends ObjectWrappedPacket<WrappedPlayInCloseWindow> {

    public WrappedPlayInCloseWindow(Object rawPacket, PacketContainer<WrappedPlayInCloseWindow> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
