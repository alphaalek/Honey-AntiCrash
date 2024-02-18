package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import me.alek.honey.netty.PacketLibrary;

public class EndInitProtocolInjector {

    public static class Initializer_v1_8 extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel channel) {
            channel.pipeline().addLast(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    PacketLibrary.get().getPipelineInjector().injectChannel(channel);
                }
            });
        }
    }

    public static class Initializer_v1_12 extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            try {
                PacketLibrary.get().getPipelineInjector().injectChannel(ctx.channel());

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
