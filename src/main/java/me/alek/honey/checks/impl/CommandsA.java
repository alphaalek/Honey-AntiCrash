package me.alek.honey.checks.impl;

import me.alek.honey.checks.types.IntervalCheck;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInChat;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.entity.Player;

public class CommandsA implements IntervalCheck, PacketCheck<WrappedPlayInChat> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInChat packet, CheckContextContainer container) {
        String message = packet.getMessage();
        if (!message.startsWith("/")) {
            return ResponseBuilder.SUCCESS;
        }
        String label = message.split(" ")[0].substring(1);
        if (((CraftServer) Bukkit.getServer()).getCommandMap().getCommand(label) == null) {
            return ResponseBuilder.SUCCESS;
        }
        for (String prohibitedCommand : ConfigurationOptions.Commands.PROHIBITED_COMMANDS) {

            if (message.equals(prohibitedCommand)) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .flag()
                        .build();
            }
        }
        return ResponseBuilder.SUCCESS;
    }

    @Override
    public ConfigurationOptions.IntervalSetting getSetting() {
        return ConfigurationOptions.Commands.get();
    }
}
