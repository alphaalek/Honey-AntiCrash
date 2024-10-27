package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class PacketListenerAdapter<WP extends WrappedPacket<WP>> {

    public void onObjectPacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {

    }

    public void onObjectPacketSend(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {

    }

    public void onBytePacketReceive(HoneyPlayer player, PacketContainer<WP> packet) throws Exception {

    }
}
