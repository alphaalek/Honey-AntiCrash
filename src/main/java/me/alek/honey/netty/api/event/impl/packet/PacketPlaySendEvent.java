package me.alek.honey.netty.api.event.impl.packet;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;

public class PacketPlaySendEvent extends PacketEvent {

    public PacketPlaySendEvent(Player player, PacketContainer<? extends WrappedPacket<?>> packet) {
        super(player, packet);
    }
}
