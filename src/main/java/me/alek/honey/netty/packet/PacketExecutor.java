package me.alek.honey.netty.packet;

import me.alek.honey.netty.packetwrappers.WrappedPacket;

import java.util.function.Predicate;

public interface PacketExecutor<WP extends WrappedPacket<WP>> extends Predicate<PacketContainer<WP>> {

    Runnable getTask(PacketContainer<WP> packetContainer);
}
