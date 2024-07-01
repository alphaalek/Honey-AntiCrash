package me.alek.honey.netty.bukkit;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.listeners.PlayerJoinQuitListener;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.util.reflection.NMS;
import org.bukkit.Bukkit;
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

        Channel channel = NMS.getChannel(player.getName());
        if (channel == null) {
            player.kickPlayer("No channel lookup found!");
            return;
        }

        channel.eventLoop().execute(() -> {
            HoneyPlayer honeyPlayer = handleChannel(player, channel);

            Bukkit.getScheduler().runTask(HoneyPlugin.get().getPlugin(), () -> {
                PlayerJoinQuitListener.onPostLoginSuccess(honeyPlayer);
            });
        });
    }

    private HoneyPlayer handleChannel(Player player, Channel channel) {

        HoneyPlayer honeyPlayer = null;
        try {
            honeyPlayer = HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(channel).getPlayer();
            honeyPlayer.initBukkitPlayer(player);

            HoneyPlayer.register(honeyPlayer, player);
        } catch (MissingHandlerException ex) {
            ex.printStackTrace();
            HoneyPlugin.get().getPlugin().getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(),
                    () -> player.kickPlayer("Broken pipe! " + ex.getMessage()));
        }

        return honeyPlayer;
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
