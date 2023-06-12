package me.alek.honey.netty.packetwrappers.play.server;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class WrappedPlayOutNamedSoundEffect extends WrappedPacket<WrappedPlayOutNamedSoundEffect> {

    public WrappedPlayOutNamedSoundEffect(Object rawPacket, PacketContainer<WrappedPlayOutNamedSoundEffect> packetContainer) {
        super(rawPacket, packetContainer);
    }
}
