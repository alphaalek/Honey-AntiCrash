package me.alek.honey.netty.bukkit;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.util.Paper;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.NMS;
import net.kyori.adventure.text.Component;
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

        Channel channel = NMS.getChannel(player.getName());
        if (channel == null) {
            kickPlayerBukkit(player);
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
            ex.printStackTrace();
            HoneyPlugin.get().getPlugin().getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(),
                    () -> kickPlayerBukkit(player));
        }
    }

    @SuppressWarnings("deprecation")
    private void kickPlayerBukkit(Player player) {
        if (Paper.IS_PAPER && NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17)) {
            player.kick(Component.text(BROKEN_PIPE_KICK_MESSAGE));
        } else {
            player.kickPlayer(BROKEN_PIPE_KICK_MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginDisallow(PlayerLoginEvent event) {
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            unregister(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        unregister(event.getPlayer());
    }

    protected void unregister(Player player) {
        HoneyPlayer honeyPlayer = HoneyPlayer.getPlayer(player.getUniqueId());
        if (honeyPlayer != null) {
            honeyPlayer.remove();
        }
        NMS.removeChannelLookup(player.getName());
    }
}
