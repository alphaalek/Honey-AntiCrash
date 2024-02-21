package me.alek.honey.netty.listener;

import io.netty.buffer.ByteBuf;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;

public class SyncPacketAdapter<WP extends WrappedPacket<WP>> {

    public void onPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {

    }

    public void onPacketSend(HoneyPlayer player, PacketContainer<WP> packetContainer) throws Exception {

    }

    public void onPacketCancel(HoneyPlayer player, PacketContainer<WP> packetContainer, PacketBound bound) {

    }

    public void onPacketError(HoneyPlayer player, PacketTypeEnum packetType, PacketContainer<WP> packetContainer) {

    }

    public void onByteBufDecode(HoneyPlayer player, ByteBuf byteBuf) {

    }
}
