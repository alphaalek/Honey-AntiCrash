package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import me.alek.honey.util.Provider;
import org.bukkit.Bukkit;

import java.util.function.Consumer;

public class ChannelProxyFactory implements Provider.ProviderTwoInput<Channel, Channel, Consumer<Channel>> {

    @Override
    public ChannelProxy get(Channel channel, Consumer<Channel> pipelineProxy) {
        return new ChannelProxy(channel, pipelineProxy);
    }

    public static class Provider implements me.alek.honey.util.Provider.ProviderNoInput<ChannelProxyFactory> {

        @Override
        public ChannelProxyFactory get() {
            return new ChannelProxyFactory();
        }
    }
}
