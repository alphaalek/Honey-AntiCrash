package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInClientCommand extends WrappedPacket<WrappedPlayInClientCommand> {
    public WrappedPlayInClientCommand(Object rawPacket, PacketContainer<WrappedPlayInClientCommand> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
