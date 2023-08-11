package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutTabComplete extends WrappedPacket<WrappedPlayOutTabComplete> {

    public WrappedPlayOutTabComplete(Object rawPacket, PacketContainer<WrappedPlayOutTabComplete> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public String[] getSuggestions() {
        return getContainer().getStringArrays().readField(0);
    }
}
