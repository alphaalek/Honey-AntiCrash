package me.alek.honey.netty.wrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.wrappers.WrappedPacket;

public class WrappedPlayInCustomPayload extends WrappedPacket<WrappedPlayInCustomPayload> {

    public WrappedPlayInCustomPayload(Object rawPacket, PacketContainer<WrappedPlayInCustomPayload> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
