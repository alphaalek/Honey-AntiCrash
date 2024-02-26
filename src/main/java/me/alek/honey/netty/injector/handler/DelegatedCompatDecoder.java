package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

interface DelegatedCompatDecoder {

    void decode(ChannelHandlerContext ctx, ByteBuf byteBufb, List<Object> list) throws Exception;
}