package me.alek.honey.netty.util;

import io.netty.channel.Channel;

public class UnsafeChannelCloser {

    public static void closeForcibly(Channel channel) {
        if (channel.isOpen()) {
            channel.unsafe().closeForcibly();
        }

        if (channel.isOpen()) {
            channel.close().syncUninterruptibly();
        }
    }
}
