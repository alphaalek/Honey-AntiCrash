package me.alek.honey.netty.handler.wrappers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;

import java.net.SocketAddress;

public class WrappedChannelOutboundHandler extends WrappedChannelHandler<ChannelOutboundHandler> implements ChannelOutboundHandler {

    public WrappedChannelOutboundHandler(ChannelOutboundHandler delegate) {
        super(delegate);
    }

    @Override
    public void bind(ChannelHandlerContext channelHandlerContext, SocketAddress socketAddress, ChannelPromise channelPromise) throws Exception {
        this.handler.bind(channelHandlerContext, socketAddress, channelPromise);
    }

    @Override
    public void connect(ChannelHandlerContext channelHandlerContext, SocketAddress socketAddress, SocketAddress socketAddress1, ChannelPromise channelPromise) throws Exception {
        this.handler.connect(channelHandlerContext, socketAddress, socketAddress1, channelPromise);
    }

    @Override
    public void disconnect(ChannelHandlerContext channelHandlerContext, ChannelPromise channelPromise) throws Exception {
        this.handler.disconnect(channelHandlerContext, channelPromise);
    }

    @Override
    public void close(ChannelHandlerContext channelHandlerContext, ChannelPromise channelPromise) throws Exception {
        this.handler.close(channelHandlerContext, channelPromise);
    }

    @Override
    public void deregister(ChannelHandlerContext channelHandlerContext, ChannelPromise channelPromise) throws Exception {
        this.handler.deregister(channelHandlerContext, channelPromise);
    }

    @Override
    public void read(ChannelHandlerContext channelHandlerContext) throws Exception {
        this.handler.read(channelHandlerContext);
    }

    @Override
    public void write(ChannelHandlerContext channelHandlerContext, Object o, ChannelPromise channelPromise) throws Exception {
        this.handler.write(channelHandlerContext, o, channelPromise);
    }

    @Override
    public void flush(ChannelHandlerContext channelHandlerContext) throws Exception {
        this.handler.flush(channelHandlerContext);
    }
}
