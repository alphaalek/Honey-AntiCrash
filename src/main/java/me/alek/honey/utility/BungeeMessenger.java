package me.alek.honey.utility;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.utility.NetProtocol;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BungeeMessenger {

    private static final String BUNGEECORD_CHANNEL = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)
            ? "bungeecord:main" : "BungeeCord";

    public static void sendPluginMessage(Player player, String subChannel, String... args) {
        if (!Bukkit.getServer().getMessenger().isIncomingChannelRegistered(HoneyPlugin.get().getPlugin(), BUNGEECORD_CHANNEL)) {
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subChannel);
        int i;
        for (i = 0; i < args.length; i ++) {
            String arg = args[i];
            out.writeUTF(arg);
        }
        player.sendPluginMessage(HoneyPlugin.get().getPlugin(), BUNGEECORD_CHANNEL, out.toByteArray());
    }
}
