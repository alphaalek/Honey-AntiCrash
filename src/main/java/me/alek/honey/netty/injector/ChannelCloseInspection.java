package me.alek.honey.netty.injector;

import io.netty.channel.Channel;

public class ChannelCloseInspection {

    public static void close(Channel channel) {
        channel.close();
    }
}
