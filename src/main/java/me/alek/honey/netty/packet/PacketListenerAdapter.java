package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public class PacketListenerAdapter<WP extends WrappedPacket<WP>> {

    public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {

    }

    public void onPacketSend(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {

    }

    public void onPacketDecode(HoneyPlayer player, PacketType type, CancellableByteBuf byteBuf) {

    }
}
