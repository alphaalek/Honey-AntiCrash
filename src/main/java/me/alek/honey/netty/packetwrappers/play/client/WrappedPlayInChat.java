package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayInChat extends ObjectWrappedPacket<WrappedPlayInChat> {

    public WrappedPlayInChat(Object rawPacket, PacketContainer<WrappedPlayInChat> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public String getMessage() {
        return getContainer().getStructure().getStrings().readField(0);
    }
}
