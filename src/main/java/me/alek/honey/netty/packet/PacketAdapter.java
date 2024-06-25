package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.CancellableByteBuf;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;

public class PacketAdapter<WP extends WrappedPacket<WP>> {

    public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {

    }

    public void onPacketSend(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {

    }

    public void onByteBufDecode(HoneyPlayer player, CancellableByteBuf byteBuf) {

    }
}
