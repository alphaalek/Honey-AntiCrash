package me.alek.honey.netty.handler.wrappers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class WrappedChannelHandler<H extends ChannelHandler> implements ChannelHandler {

    protected final H handler;

    public WrappedChannelHandler(H delegate) {
        this.handler = delegate;
    }

    public H getDelegate() {
        return this.handler;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext channelHandlerContext) {
        // to do nothing
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext channelHandlerContext) {
        // to do nothing
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {
        this.handler.exceptionCaught(channelHandlerContext, throwable);
    }
}
