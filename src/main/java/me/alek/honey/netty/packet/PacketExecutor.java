package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;

public interface PacketExecutor<WP extends WrappedPacket<WP>> {

    boolean accepts(PacketContainer<WP> packetContainer);

    Runnable getTask(PacketContainer<WP> packetContainer);
}
