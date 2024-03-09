package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutCustomPayload extends WrappedPacket<WrappedPlayOutCustomPayload> {

    public WrappedPlayOutCustomPayload(Object rawPacket, PacketContainer<WrappedPlayOutCustomPayload> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
