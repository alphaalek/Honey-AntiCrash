package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import me.alek.honey.util.Provider;
import org.bukkit.Bukkit;

import java.util.function.Consumer;

public class ChannelProxyProvider implements Provider.ProviderTwoInput<ChannelProxy, Channel, Consumer<Channel>> {

    @Override
    public ChannelProxy get(Channel channel, Consumer<Channel> pipelineProxy) {
        ChannelProxy proxy = new ChannelProxy(channel, pipelineProxy);
        Bukkit.getLogger().info(this.getClass().getClassLoader() + " " + proxy.getClass().getClassLoader());
        // TODO: ser ud til stadig ikke at virke
        return proxy;
    }
}
