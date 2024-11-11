package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;

public class WrappedPlayInTabComplete extends ObjectWrappedPacket<WrappedPlayInTabComplete> {

    public WrappedPlayInTabComplete(Object rawPacket, PacketContainer<WrappedPlayInTabComplete> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public String getMessage() {
        return getContainer().getStructure().getStrings().readField(0);
    }
}
