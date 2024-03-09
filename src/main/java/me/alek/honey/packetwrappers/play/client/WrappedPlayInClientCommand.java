package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayInClientCommand extends WrappedPacket<WrappedPlayInClientCommand> {
    public WrappedPlayInClientCommand(Object rawPacket, PacketContainer<WrappedPlayInClientCommand> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
