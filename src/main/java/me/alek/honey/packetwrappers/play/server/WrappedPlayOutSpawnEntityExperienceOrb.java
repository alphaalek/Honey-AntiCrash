package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutSpawnEntityExperienceOrb extends WrappedPacket<WrappedPlayOutSpawnEntityExperienceOrb> {

    public WrappedPlayOutSpawnEntityExperienceOrb(Object rawPacket, PacketContainer<WrappedPlayOutSpawnEntityExperienceOrb> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
