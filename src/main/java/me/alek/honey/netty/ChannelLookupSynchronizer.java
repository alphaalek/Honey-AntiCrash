package me.alek.honey.netty;

import io.netty.channel.Channel;

import java.util.HashMap;

public class ChannelLookupSynchronizer {

    private static final HashMap<String, Channel> lookupChannels = new HashMap<>();

    public static synchronized Channel getChannel(String name) {
        return lookupChannels.get(name);
    }

    public static synchronized void submitChannelForName(String name, Channel channel) {
        lookupChannels.put(name, channel);
    }

    public static void removeChannelLookup(String name) {
        lookupChannels.remove(name);
    }
}
