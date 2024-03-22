package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import org.bukkit.Bukkit;

public class EndInitProtocolInjector {

    public static class Initializer_v1_8 extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel channel) {
            channel.pipeline().addLast(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    HoneyPlugin.get().getPacketLibrary().getPipelineInjector().injectChannel(channel);
                }
            });
        }
    }

    public static class Initializer_v1_12 extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            try {
                HoneyPlugin.get().getPacketLibrary().getPipelineInjector().injectChannel(ctx.channel());

            } catch (Exception ex) {
                ex.printStackTrace();
                ctx.close();

                if (ctx.pipeline().context(this) != null) {
                    ctx.pipeline().remove(this);
                }
            }
            super.channelRegistered(ctx);
        }
    }
}
