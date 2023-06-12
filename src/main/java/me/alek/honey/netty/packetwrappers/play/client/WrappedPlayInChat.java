package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInChat extends WrappedPacket<WrappedPlayInChat> {

    public WrappedPlayInChat(Object rawPacket, PacketContainer<WrappedPlayInChat> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public String getMessage() {
        return getPacket().getStrings().read(0);
    }
}
