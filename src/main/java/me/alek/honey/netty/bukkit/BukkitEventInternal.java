package me.alek.honey.netty.bukkit;

import io.netty.channel.Channel;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.injector.AbstractNettyChannelInjector;
import me.alek.honey.netty.injector.AbstractPipelineProxy;
import me.alek.honey.netty.injector.CommonChannelInjector;
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
        Player player = event.getPlayer();

        PacketLibrary library = PacketLibrary.get();
        Channel channel = NMSUtils.getChannel(player);

        if (channel == null || channel.pipeline().toMap().size() == 0) {

            player.kickPlayer("Error occurred when injecting pipeline. Try connecting again.");
            return;
        }

        AbstractNettyChannelInjector injector = library.getInjector();

        if (!library.useLateInjection()) {
            try {
                CommonChannelInjector.getInjectedLatch().await();

            } catch (Exception ex) {
                // interrupted, so the pipeline was never done injecting
                ex.printStackTrace();

                channel.close();
            }
        }
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
        if (!injected) injector.injectPlayer(player, true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        PacketLibrary.get().getInjector().ejectPlayer(player);
        NMSUtils.removeChannelLookup(player.getUniqueId());
    }
}
