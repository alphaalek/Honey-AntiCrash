package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutCombatEvent extends WrappedPacket<WrappedPlayOutCombatEvent> {

    public WrappedPlayOutCombatEvent(Object rawPacket, PacketContainer<WrappedPlayOutCombatEvent> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
