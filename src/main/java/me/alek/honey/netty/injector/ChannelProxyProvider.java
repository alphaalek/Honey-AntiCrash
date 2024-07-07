package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import me.alek.honey.util.Provider;

import java.util.function.Consumer;

public class ChannelProxyProvider implements Provider.ProviderTwoInput<ChannelProxy, Channel, Consumer<Channel>> {

    @Override
    public ChannelProxy get(Channel channel, Consumer<Channel> pipelineProxy) {
        return new ChannelProxy(channel, pipelineProxy);
    }
}
