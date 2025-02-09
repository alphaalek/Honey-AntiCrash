package me.alek.honey.netty;

import io.netty.channel.Channel;

public class ChannelCloseInspection {

    public static void close(Channel channel) {
        try {
            Object b = null;
            b.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        channel.close();
    }
}
