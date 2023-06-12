package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInCloseWindow extends WrappedPacket<WrappedPlayInCloseWindow> {

    public WrappedPlayInCloseWindow(Object rawPacket, PacketContainer<WrappedPlayInCloseWindow> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
