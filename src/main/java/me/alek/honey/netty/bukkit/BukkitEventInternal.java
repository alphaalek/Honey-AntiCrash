package me.alek.honey.netty.bukkit;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.ChannelLookupSynchronizer;
import me.alek.honey.netty.injector.MissingHandlerException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitEventInternal implements Listener {

    private static final String BROKEN_PIPE_KICK_MESSAGE = "Broken pipe! Try reconnecting.";

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Channel channel = ChannelLookupSynchronizer.getChannel(player.getName());
        if (channel == null) {
            HoneyPlayer.kick(player, BROKEN_PIPE_KICK_MESSAGE);
            return;
        }

        channel.eventLoop().execute(() -> handleChannel(player, channel));
    }

    private void handleChannel(Player player, Channel channel) {
        try {
            HoneyPlayer honeyPlayer = HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(channel).getPlayer();
            honeyPlayer.initBukkitPlayer(player);

            HoneyPlayer.register(honeyPlayer, player);
        } catch (MissingHandlerException ex) {
            HoneyPlugin.get().getPlugin().getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(),
                    () -> HoneyPlayer.kick(player, BROKEN_PIPE_KICK_MESSAGE));
        }
    }
}
