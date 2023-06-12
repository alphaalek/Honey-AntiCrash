package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInBlockPlace extends WrappedPacket<WrappedPlayInBlockPlace> {

    public WrappedPlayInBlockPlace(Object rawPacket, PacketContainer<WrappedPlayInBlockPlace> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public Object getItem() {
       return getPacket().getItems().read(0);
    }

}
