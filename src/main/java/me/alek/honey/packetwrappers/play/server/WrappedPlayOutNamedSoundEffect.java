package me.alek.honey.packetwrappers.play.server;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public class WrappedPlayOutNamedSoundEffect extends WrappedPacket<WrappedPlayOutNamedSoundEffect> {

    public WrappedPlayOutNamedSoundEffect(Object rawPacket, PacketContainer<WrappedPlayOutNamedSoundEffect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
