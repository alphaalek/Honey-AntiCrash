package me.alek.honey.netty;

import io.netty.channel.Channel;
import org.bukkit.Bukkit;

public class UnsafeChannelCloser {

    public static void closeForcibly(Channel channel) {
        channel.config().setAutoRead(false);

        if (channel.isOpen()) {
            channel.unsafe().closeForcibly();
        }

        if (channel.isOpen()) {
            channel.close().syncUninterruptibly();
        }
    }
}
