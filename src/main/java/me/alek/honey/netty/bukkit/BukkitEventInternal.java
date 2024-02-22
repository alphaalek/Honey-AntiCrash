package me.alek.honey.netty.bukkit;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.injector.connection.SpigotPipelineInjector;
import me.alek.honey.netty.injector.handler.DuplexChannelHandler;
import me.alek.honey.netty.injector.handler.InboundPacketDecoder;
import me.alek.honey.netty.utility.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitEventInternal implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Channel channel = NMSUtils.getChannel(player);

        if (channel == null || channel.pipeline().toMap().isEmpty()) {
            player.kickPlayer("Error occurred when injecting pipeline. Try connecting again. (response: " + ((channel == null) ? 1 : 2 + ")"));
            return;
        }
        HoneyPlayer honeyPlayer = HoneyPlayer.registerPlayer(player);

        SpigotPipelineInjector injector = HoneyPlugin.get().getPacketLibrary().getPipelineInjector();
        try {
            injector.updatePlayer(honeyPlayer);
        } catch (MissingHandlerException exception) {
            player.kickPlayer("Broken pipe!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ejectChannel(NMSUtils.getChannel(player));
        NMSUtils.removeChannelLookup(player.getUniqueId());

        HoneyPlayer.removePlayer(player.getUniqueId());
    }
}
