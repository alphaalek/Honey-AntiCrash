package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutCombatEvent extends WrappedPacket<WrappedPlayOutCombatEvent> {

    public WrappedPlayOutCombatEvent(Object rawPacket, PacketContainer<WrappedPlayOutCombatEvent> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
