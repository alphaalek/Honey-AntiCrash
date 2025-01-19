package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public interface PacketListener<WP extends WrappedPacket<WP>> {

    void onObjectPacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception;

    void onObjectPacketSend(HoneyPlayer player, PacketContainer<WP> packet) throws Exception;

    void onBytePacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception;
}
