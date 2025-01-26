package me.alek.honey.async;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packetwrappers.impl.WrappedPlayOutDisconnect;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DebugCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, String @NotNull [] strings) {
        WorkerPool.ASYNC_PACKET_DECODER_POOL.sendDebug(sender);


        HoneyPlayer pl = HoneyPlayer.getPlayer(((Player)sender).getUniqueId());
        pl.online = false;
        WrappedPlayOutDisconnect packet = WrappedPlayOutDisconnect.of(pl.packetHandler.getProtocolState(PacketBound.CLIENT), Component.text("test"));
        packet.write(pl);
        return true;
    }
}
