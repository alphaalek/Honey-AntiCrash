package me.alek.honey.netty.bukkit;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.listeners.PlayerJoinListener;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.injector.connection.SpigotPipelineInjector;
import me.alek.honey.netty.injector.handler.InboundPacketFilter;
import me.alek.honey.netty.utility.NMS;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.CompletableFuture;

public class BukkitEventInternal implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Channel channel = NMS.getChannelIfSet(player.getUniqueId());
        if (channel != null) {
            handleChannel(player, channel);
            return;
        }

        NMS.getChannel(player.getUniqueId()).thenAccept((ch) -> handleChannel(player, ch));
    }

    private void handleChannel(Player player, Channel channel) {
        channel.eventLoop().execute(() -> {
            if (channel.pipeline().toMap().isEmpty()) {
                HoneyPlugin.get().getPlugin().getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(),
                        () -> player.kickPlayer("Error occurred when injecting pipeline. Try connecting again."));
                return;
            }

            HoneyPlayer honeyPlayer = HoneyPlayer.registerPlayer(player);

            SpigotPipelineInjector injector = HoneyPlugin.get().getPacketLibrary().getPipelineInjector();
            try {
                injector.updatePlayer(honeyPlayer);
            } catch (MissingHandlerException exception) {
                HoneyPlugin.get().getPlugin().getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(),
                        () -> player.kickPlayer("Broken pipe!"));
            }

            PlayerJoinListener.onPostLoginSuccess(honeyPlayer);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Channel channel = NMS.getChannelIfSet(player.getUniqueId());
        if (channel != null) {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ejectChannel(channel);
        }
        NMS.removeChannelLookup(player.getUniqueId());

        HoneyPlayer.removePlayer(player.getUniqueId());
    }
}
