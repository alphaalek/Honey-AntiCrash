package me.alek.honey.netty.injector;

import io.netty.channel.*;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.UnsafeChannelCloser;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class EndInitProtocolInjector {

    public static class Initializer_v1_8 extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel channel) {
            channel.pipeline().addLast(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    handleChannelInitialization(this, channel);
                }
            });
        }
    }

    public static class Initializer_v1_12 extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            handleChannelInitialization(this, ctx.channel());

            super.channelRegistered(ctx);
        }
    }

    private static void handleChannelInitialization(ChannelHandler handler, Channel channel) {
        try {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().injectChannel(channel);

        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Error occurred when initializing channel.", ex);

            if (channel.pipeline().context(handler) != null) {
                channel.pipeline().remove(handler);
            }

            UnsafeChannelCloser.closeForcibly(channel);
        }
    }
}
