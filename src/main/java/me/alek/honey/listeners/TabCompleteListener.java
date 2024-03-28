package me.alek.honey.listeners;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutTabComplete;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabCompleteListener extends SyncPacketAdapter<WrappedPlayOutTabComplete> {

    private boolean shouldBlockTabCompletion(HoneyPlayer player) {
        if (ConfigurationOptions.General.HIDE_COMMAND_ENABLED) {
            return !player.player.hasPermission(ConfigurationOptions.General.HIDE_BYPASS_PERMISSION);
        }
        return false;
    }

    @Override
    public void onPacketSend(HoneyPlayer player, PacketContainer<WrappedPlayOutTabComplete> packetContainer) {
        if (shouldBlockTabCompletion(player)) {
            String[] suggestions = packetContainer.getPacket().getSuggestions();
            for (String suggestion : suggestions) {
                if (suggestion.startsWith("/honey")) {
                    packetContainer.getPacket().removeSuggestion(suggestion);
                }
            }
        }
    }
}
