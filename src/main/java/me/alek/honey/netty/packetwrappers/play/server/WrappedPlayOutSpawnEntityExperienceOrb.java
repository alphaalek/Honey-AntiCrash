package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutSpawnEntityExperienceOrb extends WrappedPacket<WrappedPlayOutSpawnEntityExperienceOrb> {

    public WrappedPlayOutSpawnEntityExperienceOrb(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityExperienceOrb> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
