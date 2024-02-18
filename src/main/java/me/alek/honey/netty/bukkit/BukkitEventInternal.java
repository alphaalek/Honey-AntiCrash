package me.alek.honey.netty.bukkit;

import io.netty.channel.Channel;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.injector.MissingHoneyHandlerException;
import me.alek.honey.netty.injector.connection.SpigotPipelineInjector;
import me.alek.honey.netty.injector.handler.DuplexChannelHandler;
import me.alek.honey.netty.injector.handler.InboundPacketDecoder;
import me.alek.honey.netty.utility.NMSUtils;
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

        SpigotPipelineInjector injector = library.getPipelineInjector();
        try {
            injector.updatePlayer(player);
        } catch (MissingHoneyHandlerException exception) {
            player.kickPlayer("Broken pipe!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        PacketLibrary.get().getPipelineInjector().ejectChannel(NMSUtils.getChannel(player));
        NMSUtils.removeChannelLookup(player.getUniqueId());

        DuplexChannelHandler.getPacketBundleSavers().remove(player.getUniqueId());
        InboundPacketDecoder.unregisterPlayer(player);
    }
}
