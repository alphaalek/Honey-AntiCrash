package me.alek.honey.checks.impl;

import me.alek.honey.checks.PacketCheck;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInUpdateSign;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class IllegalSign extends AsyncPacketAdapter<WrappedPlayInUpdateSign> implements PacketCheck {

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInUpdateSign> packetContainer) {
        for (String line : packetContainer.getPacket().getLines()) {
            if (line.length() > 49) {
                punish(player, packetContainer, "Illegal Component");
            }
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.UPDATE_SIGN);
    }

    @Override
    public String getName() {
        return "Sign Update";
    }
}
