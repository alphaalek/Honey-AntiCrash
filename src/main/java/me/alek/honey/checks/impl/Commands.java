package me.alek.honey.checks.impl;

import me.alek.honey.checks.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInChat;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class Commands extends AsyncPacketAdapter<WrappedPlayInChat> implements PacketCheck {

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInChat> packetContainer) {
        String message = packetContainer.getPacket().getMessage();
        if (!message.startsWith("/")) {
            return;
        }
        String label = message.split(" ")[0].substring(1);
        if (((CraftServer)Bukkit.getServer()).getCommandMap().getCommand(label) == null) {
            return;
        }
        for (String prohibitedCommand : ConfigurationOptions.Commands.PROHIBITED_COMMANDS) {
            if (message.equals(prohibitedCommand)) {
                flag(player, packetContainer, this, "Commands", ConfigurationOptions.Commands.get());
                return;
            }
        }
    }


    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CHAT);
    }

    @Override
    public String getName() {
        return "Commands";
    }
}
