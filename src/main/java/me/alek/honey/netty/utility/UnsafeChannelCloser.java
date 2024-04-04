package me.alek.honey.netty.utility;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import me.alek.honey.HoneyPlugin;

import java.util.Iterator;
import java.util.Map;

public class UnsafeChannelCloser {

    public static void closeForcibly(Channel channel) {
        if (channel.isOpen()) {

            if (!channel.isOpen()) {
                Iterator<Map.Entry<String, ChannelHandler>> handlers = channel.pipeline().iterator();
                ChannelHandler handler;
                while (handlers.hasNext()) {
                    handler = handlers.next().getValue();

                    if (handler instanceof ChannelInboundHandler) {
                        ChannelHandlerContext ctx = channel.pipeline().context(handler);
                        try {
                            ((ChannelInboundHandler)handler).channelInactive(ctx);
                        } catch (Exception ex) {
                            try {
                                handler.exceptionCaught(ctx, ex);
                            } catch (Exception ignored) {

                            }
                        }
                    }
                }
            }
            channel.unsafe().closeForcibly();
        }

        if (channel.isOpen()) {
            channel.close().syncUninterruptibly();
        }
    }
}
