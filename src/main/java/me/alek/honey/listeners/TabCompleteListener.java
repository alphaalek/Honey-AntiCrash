package me.alek.honey.listeners;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketListenerAdapter;
import me.alek.honey.netty.packetwrappers.play.server.WrappedPlayOutTabComplete;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.stream.Collectors;

public class TabCompleteListener extends PacketListenerAdapter<WrappedPlayOutTabComplete> implements Listener {

    private boolean shouldBlockTabCompletion(HoneyPlayer player) {
        if (ConfigurationOptions.General.HIDE_COMMAND_ENABLED) {
            return !player.bukkit.hasPermission(ConfigurationOptions.General.HIDE_BYPASS_PERMISSION);
        }

        return false;
    }

    @Override
    public void onObjectPacketSend(HoneyPlayer player, PacketContainer<WrappedPlayOutTabComplete> packetContainer) {
        if (shouldBlockTabCompletion(player)) {
            String[] suggestions = packetContainer.getAnyWrapped().getSuggestions();
            for (String suggestion : suggestions) {
                if (suggestion.startsWith("/honey")) {
                    packetContainer.getAnyWrapped().removeSuggestion(suggestion);
                }
            }
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (event.getSender() instanceof Player) {
            Player player = (Player) event.getSender();
            if (shouldBlockTabCompletion(HoneyPlayer.getPlayer(player.getUniqueId()))) {
                event.setCompletions(event.getCompletions().stream().filter(c -> !c.startsWith("honey")).collect(Collectors.toList()));
            }
        }
    }
}
