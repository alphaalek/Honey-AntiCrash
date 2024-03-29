package me.alek.honey.listeners;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.listener.PacketAdapter;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutTabComplete;

public class TabCompleteListener extends PacketAdapter<WrappedPlayOutTabComplete> {

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
