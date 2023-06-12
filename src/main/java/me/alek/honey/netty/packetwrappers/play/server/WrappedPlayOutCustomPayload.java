package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutCustomPayload extends WrappedPacket<WrappedPlayOutCustomPayload> {

    public WrappedPlayOutCustomPayload(Object rawPacket, PacketContainer<WrappedPlayOutCustomPayload> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
