package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutSelectAdvancementTab extends WrappedPacket<WrappedPlayOutSelectAdvancementTab> {

    public WrappedPlayOutSelectAdvancementTab(Object rawPacket, PacketContainer<WrappedPlayOutSelectAdvancementTab> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
