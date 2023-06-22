package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInTabComplete extends WrappedPacket<WrappedPlayInTabComplete> {

    public WrappedPlayInTabComplete(Object rawPacket, PacketContainer<WrappedPlayInTabComplete> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public String getMessage() {
        return getContainer().getStrings().readField(0);
    }
}
