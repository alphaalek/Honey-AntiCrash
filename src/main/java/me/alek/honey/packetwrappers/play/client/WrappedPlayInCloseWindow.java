package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInCloseWindow extends WrappedPacket<WrappedPlayInCloseWindow> {

    public WrappedPlayInCloseWindow(Object rawPacket, PacketContainer<WrappedPlayInCloseWindow> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
