package me.alek.honey.netty;

import io.netty.channel.Channel;

public class ChannelCloseInspection {

    public static void close(Channel channel) {
        channel.close();
    }
}
