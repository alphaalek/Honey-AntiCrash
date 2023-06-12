package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutSelectAdvancementTab extends WrappedPacket<WrappedPlayOutSelectAdvancementTab> {

    public WrappedPlayOutSelectAdvancementTab(Object rawPacket, PacketContainer<WrappedPlayOutSelectAdvancementTab> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
