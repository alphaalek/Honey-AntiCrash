package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.IntervalCheck;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInTabComplete;
import org.bukkit.entity.Player;

public class TabA implements PacketCheck<WrappedPlayInTabComplete>, IntervalCheck {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInTabComplete packet, CheckContextContainer container) {
        String label = packet.getMessage().split(" ")[0];

        for (String prohibitedCommand : ConfigurationOptions.Commands.PROHIBITED_TAB_COMPLETE_COMMANDS) {
            if (label.equals(prohibitedCommand)) {
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
