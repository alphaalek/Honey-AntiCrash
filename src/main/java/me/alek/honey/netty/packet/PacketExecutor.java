package me.alek.honey.netty.packet;

import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;

import java.util.function.Predicate;

public interface PacketExecutor<WP extends SourcedWrappedPacket<WP>> extends Predicate<PacketContainer<WP>> {

    Runnable getTask(PacketContainer<WP> packetContainer);
}
