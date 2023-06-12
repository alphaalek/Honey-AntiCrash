package me.alek.honey.netty.api.event.impl.packet;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;

public class PacketStatusSendEvent extends PacketEvent {

    public PacketStatusSendEvent(Player player, PacketContainer<? extends WrappedPacket<?>> packet) {
        super(player, packet);
    }
}
