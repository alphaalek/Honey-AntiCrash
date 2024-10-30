package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;

public class WrappedPlayInBookEdit extends ObjectWrappedPacket<WrappedPlayInBookEdit> {

    public WrappedPlayInBookEdit(Object rawPacket, PacketContainer<WrappedPlayInBookEdit> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
