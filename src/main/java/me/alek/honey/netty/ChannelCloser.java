package me.alek.honey.netty;

import io.netty.channel.Channel;

public class ChannelCloser {

    public static void closeForcibly(Channel channel) {
        if (channel.isOpen()) {
            channel.unsafe().closeForcibly();
        }

        if (channel.isOpen()) {
            channel.close().syncUninterruptibly();
        }
    }
}
