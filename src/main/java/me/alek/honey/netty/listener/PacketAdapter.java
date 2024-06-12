package me.alek.honey.netty.listener;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.cancellables.CancellableByteBufImpl;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;

public class PacketAdapter<WP extends WrappedPacket<WP>> {

    public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {

    }

    public void onPacketSend(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {

    }

    public void onPacketCancel(HoneyPlayer player, PacketContainer<WP> packetContainer, PacketBound bound) {

    }

    public void onPacketError(HoneyPlayer player, PacketType packetType, PacketContainer<WP> packetContainer) {

    }

    public void onByteBufDecode(HoneyPlayer player, CancellableByteBufImpl byteBuf) {

    }
}
