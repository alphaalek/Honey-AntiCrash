package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public interface PacketReader<WP extends WrappedPacket<WP>> {

    Object[] read(PacketContainer<WP> packetContainer);
}
