package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInCloseWindow extends WrappedPacket<WrappedPlayInCloseWindow> {

    public WrappedPlayInCloseWindow(Object rawPacket, PacketContainer<WrappedPlayInCloseWindow> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
