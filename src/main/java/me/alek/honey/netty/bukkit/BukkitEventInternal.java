package me.alek.honey.netty.bukkit;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.listeners.PlayerJoinQuitListener;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.injector.SpigotPipelineInjector;
import me.alek.honey.netty.util.NMS;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitEventInternal implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Channel channel = NMS.getChannelIfSet(player.getName());
        if (channel == null) {
            player.kickPlayer("No channel lookup found!");
            return;
        }

        handleChannel(player, channel);
    }

    private void handleChannel(Player player, Channel channel) {
        channel.eventLoop().execute(() -> {
            HoneyPlayer honeyPlayer = HoneyPlayer.registerPlayer(player);

            SpigotPipelineInjector injector = HoneyPlugin.get().getPacketLibrary().getPipelineInjector();
            try {
                injector.updatePlayer(honeyPlayer);
            } catch (MissingHandlerException exception) {
                HoneyPlugin.get().getPlugin().getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(),
                        () -> player.kickPlayer("Broken pipe! " + exception.getMessage()));
                return;
            }

            PlayerJoinQuitListener.onPostLoginSuccess(honeyPlayer);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginDisallow(PlayerLoginEvent event) {
        if(!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            unregister(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        unregister(event.getPlayer());
    }

    protected void unregister(Player player) {
        HoneyPlayer.removePlayer(player.getUniqueId());
        NMS.removeChannelLookup(player.getName());
    }
}
