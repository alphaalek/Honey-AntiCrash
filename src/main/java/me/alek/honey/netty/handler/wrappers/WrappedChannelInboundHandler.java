package me.alek.honey.netty.handler.wrappers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;

public class WrappedChannelInboundHandler extends WrappedChannelHandler<ChannelInboundHandler> implements ChannelInboundHandler {

    public WrappedChannelInboundHandler(ChannelInboundHandler delegate) {
        super(delegate);
    }

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        this.handler.channelRead(channelHandlerContext, o);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext channelHandlerContext) throws Exception {
        this.handler.channelReadComplete(channelHandlerContext);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        this.handler.userEventTriggered(channelHandlerContext, o);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext channelHandlerContext) throws Exception {
        this.handler.channelWritabilityChanged(channelHandlerContext);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext channelHandlerContext) throws Exception {
        this.handler.channelRegistered(channelHandlerContext);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext channelHandlerContext) throws Exception {
        this.handler.channelUnregistered(channelHandlerContext);
    }

    @Override
    public void channelActive(ChannelHandlerContext channelHandlerContext) throws Exception {
        this.handler.channelActive(channelHandlerContext);
    }

    @Override
    public void channelInactive(ChannelHandlerContext channelHandlerContext) throws Exception {
        this.handler.channelActive(channelHandlerContext);
    }
}
