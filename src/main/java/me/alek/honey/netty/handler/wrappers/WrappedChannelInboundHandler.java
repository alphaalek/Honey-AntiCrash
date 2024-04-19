package me.alek.honey.netty.handler.wrappers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;

public class WrappedChannelInboundHandler implements ChannelInboundHandler {

    private final ChannelInboundHandler handler;

    public WrappedChannelInboundHandler(ChannelInboundHandler delegate) {
        this.handler= delegate;
    }

    public ChannelInboundHandler getDelegate() {
        return handler;
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        handler.channelRead(channelHandlerContext, o);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext channelHandlerContext) throws Exception {
        handler.channelRegistered(channelHandlerContext);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext channelHandlerContext) throws Exception {
        handler.channelRegistered(channelHandlerContext);
    }

    @Override
    public void channelActive(ChannelHandlerContext channelHandlerContext) throws Exception {
        handler.channelActive(channelHandlerContext);
    }

    @Override
    public void channelInactive(ChannelHandlerContext channelHandlerContext) throws Exception {
        handler.channelInactive(channelHandlerContext);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext channelHandlerContext) throws Exception {
        handler.channelReadComplete(channelHandlerContext);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        handler.userEventTriggered(channelHandlerContext, o);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext channelHandlerContext) throws Exception {
        handler.channelWritabilityChanged(channelHandlerContext);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext channelHandlerContext) throws Exception {
        handler.handlerAdded(channelHandlerContext);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext channelHandlerContext) throws Exception {
        handler.handlerRemoved(channelHandlerContext);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {
        handler.exceptionCaught(channelHandlerContext, throwable);
    }
}
