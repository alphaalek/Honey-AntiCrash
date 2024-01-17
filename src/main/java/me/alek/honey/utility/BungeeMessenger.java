package me.alek.honey.utility;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.alek.honey.HoneyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BungeeMessenger {

    public static void sendPluginMessage(Player player, String subChannel, String... args) {
        if (!Bukkit.getServer().getMessenger().isIncomingChannelRegistered(HoneyPlugin.get().getPlugin(), "BungeeCoord")) {
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subChannel);
        int i;
        for (i = 0; i < args.length; i ++) {
            String arg = args[i];
            out.writeUTF(arg);
        }
        player.sendPluginMessage(HoneyPlugin.get().getPlugin(), "BungeeCord", out.toByteArray());
    }
}
