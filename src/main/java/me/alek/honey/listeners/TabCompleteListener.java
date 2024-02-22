package me.alek.honey.listeners;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutTabComplete;

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
            final List<String> suggestions = packetContainer.getPacket().getControllableSuggestions();

            if (suggestions.contains("/honey")) {
                suggestions.remove("/honey");
                suggestions.remove("/honey:honey");
            }
        }
    }
}
