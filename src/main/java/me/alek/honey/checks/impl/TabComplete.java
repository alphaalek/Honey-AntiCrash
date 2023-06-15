package me.alek.honey.checks.impl;

import me.alek.honey.checks.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInTabComplete;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class TabComplete extends AsyncPacketAdapter<WrappedPlayInTabComplete> implements PacketCheck {

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInTabComplete> packetContainer) {
        String message = packetContainer.getPacket().getMessage();
        if (!message.startsWith("/")) {
            return;
        }
        String label = packetContainer.getPacket().getMessage().split(" ")[0];

        for (String prohibitedCommand : ConfigurationOptions.Commands.PROHIBITED_TAB_COMPLETE_COMMANDS) {
            if (label.equals(prohibitedCommand)) {
                flag(player, packetContainer, this, "Illegal Message", ConfigurationOptions.Commands.get());
                return;
            }
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.TAB_COMPLETE);
    }

    @Override
    public String getName() {
        return "Tab Complete";
    }

}
