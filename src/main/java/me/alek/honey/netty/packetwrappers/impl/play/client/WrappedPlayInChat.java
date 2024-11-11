package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;

public class WrappedPlayInChat extends ObjectWrappedPacket<WrappedPlayInChat> {

    public WrappedPlayInChat(Object rawPacket, PacketContainer<WrappedPlayInChat> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public String getMessage() {
        return getContainer().getStructure().getStrings().readField(0);
    }
}
