package me.alek.honey.netty.bukkit;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.injector.AbstractNettyChannelInjector;
import me.alek.honey.netty.injector.AbstractPipelineProxy;
import me.alek.honey.netty.injector.CommonChannelInjector;
import me.alek.honey.netty.injector.DuplexChannelHandler;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitEventInternal implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        onJoin(event, true);
    }

    public void onJoin(PlayerJoinEvent event, boolean firstCall) {
        Player player = event.getPlayer();

        PacketLibrary library = PacketLibrary.get();
        Channel channel = NMSUtils.getChannel(player);
        if ((channel == null || channel.pipeline().toMap().isEmpty()) && firstCall) {
            NMSUtils.removeChannelLookup(player.getUniqueId());
            onJoin(event, false);
            return;
        }
        if (channel == null || channel.pipeline().toMap().isEmpty()) {
            player.kickPlayer("Error occurred when injecting pipeline. Try connecting again. (response: " + ((channel == null) ? 1 : 2 + ")"));
            return;
        }

        AbstractNettyChannelInjector injector = library.getInjector();
        boolean injected = false;
        if (!library.useLateInjection()) {

            AbstractPipelineProxy packetHandler = injector.getHandler(channel);
            if (packetHandler != null) {
                injected = true;
                packetHandler.setPlayer(player);

                // trigger events
                injector.injectPlayer(player, false);
            }
        }
        // late inject player's channel if it's not injected yet
        if (!injected) injector.injectPlayer(player, true);

        if (injector.getHandler(channel) == null) {
            player.kickPlayer("Broken pipe!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        PacketLibrary.get().getInjector().ejectPlayer(player);
        NMSUtils.removeChannelLookup(player.getUniqueId());
        DuplexChannelHandler.getLastPacketMap().remove(player.getUniqueId());
    }
}
