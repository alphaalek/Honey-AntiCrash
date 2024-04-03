package me.alek.honey.netty.injector;

import io.netty.channel.*;
import io.netty.util.concurrent.EventExecutorGroup;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.connection.SpigotPipelineInjector;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PipelineProxy implements ChannelPipeline {

    private final ChannelPipeline delegate;
    private final Channel channel;
    private final SpigotPipelineInjector spigotPipelineInjector = HoneyPlugin.get().getPacketLibrary().getPipelineInjector();

    public PipelineProxy(ChannelPipeline delegate) {
        this.channel = delegate.channel();
        this.delegate = delegate;
    }

    @Override
    public ChannelPipeline addFirst(String s, ChannelHandler channelHandler) {
        ChannelPipeline pipeline = delegate.addFirst(s, channelHandler);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline addFirst(EventExecutorGroup eventExecutorGroup, String s, ChannelHandler channelHandler) {
        ChannelPipeline pipeline = delegate.addFirst(eventExecutorGroup, s, channelHandler);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline addLast(String s, ChannelHandler channelHandler) {
        ChannelPipeline pipeline = delegate.addLast(s, channelHandler);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline addLast(EventExecutorGroup eventExecutorGroup, String s, ChannelHandler channelHandler) {
        ChannelPipeline pipeline = delegate.addLast(eventExecutorGroup, s, channelHandler);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline addBefore(String s, String s1, ChannelHandler channelHandler) {
        ChannelPipeline pipeline = delegate.addBefore(s, s1, channelHandler);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline addBefore(EventExecutorGroup eventExecutorGroup, String s, String s1, ChannelHandler channelHandler) {
        ChannelPipeline pipeline = delegate.addBefore(eventExecutorGroup, s, s1, channelHandler);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline addAfter(String s, String s1, ChannelHandler channelHandler) {
        ChannelPipeline pipeline = delegate.addAfter(s, s1, channelHandler);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline addAfter(EventExecutorGroup eventExecutorGroup, String s, String s1, ChannelHandler channelHandler) {
        ChannelPipeline pipeline = delegate.addAfter(eventExecutorGroup, s, s1, channelHandler);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline addFirst(ChannelHandler... channelHandlers) {
        ChannelPipeline pipeline = delegate.addFirst(channelHandlers);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline addFirst(EventExecutorGroup eventExecutorGroup, ChannelHandler... channelHandlers) {
        ChannelPipeline pipeline = delegate.addFirst(eventExecutorGroup, channelHandlers);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline addLast(ChannelHandler... channelHandlers) {
        ChannelPipeline pipeline = delegate.addLast(channelHandlers);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline addLast(EventExecutorGroup eventExecutorGroup, ChannelHandler... channelHandlers) {
        ChannelPipeline pipeline = delegate.addFirst(eventExecutorGroup, channelHandlers);
        spigotPipelineInjector.ensureCorrectPipelineOrder(channel);
        return pipeline;
    }

    @Override
    public ChannelPipeline remove(ChannelHandler channelHandler) {
        return delegate.remove(channelHandler);
    }

    @Override
    public ChannelHandler remove(String s) {
        return delegate.remove(s);
    }

    @Override
    public <T extends ChannelHandler> T remove(Class<T> aClass) {
        return delegate.remove(aClass);
    }

    @Override
    public ChannelHandler removeFirst() {
        return delegate.removeFirst();
    }

    @Override
    public ChannelHandler removeLast() {
        return delegate.removeLast();
    }

    @Override
    public ChannelPipeline replace(ChannelHandler channelHandler, String s, ChannelHandler channelHandler1) {
        return delegate.replace(channelHandler, s, channelHandler1);
    }

    @Override
    public ChannelHandler replace(String s, String s1, ChannelHandler channelHandler) {
        return delegate.replace(s, s1, channelHandler);
    }

    @Override
    public <T extends ChannelHandler> T replace(Class<T> aClass, String s, ChannelHandler channelHandler) {
        return delegate.replace(aClass, s, channelHandler);
    }

    @Override
    public ChannelHandler first() {
        return delegate.first();
    }

    @Override
    public ChannelHandlerContext firstContext() {
        return delegate.firstContext();
    }

    @Override
    public ChannelHandler last() {
        return delegate.last();
    }

    @Override
    public ChannelHandlerContext lastContext() {
        return delegate.lastContext();
    }

    @Override
    public ChannelHandler get(String s) {
        return delegate.get(s);
    }

    @Override
    public <T extends ChannelHandler> T get(Class<T> aClass) {
        return delegate.get(aClass);
    }

    @Override
    public ChannelHandlerContext context(ChannelHandler channelHandler) {
        return delegate.context(channelHandler);
    }

    @Override
    public ChannelHandlerContext context(String s) {
        return delegate.context(s);
    }

    @Override
    public ChannelHandlerContext context(Class<? extends ChannelHandler> aClass) {
        return delegate.context(aClass);
    }

    @Override
    public Channel channel() {
        return delegate.channel();
    }

    @Override
    public List<String> names() {
        return delegate.names();
    }

    @Override
    public Map<String, ChannelHandler> toMap() {
        return delegate.toMap();
    }

    @Override
    public ChannelPipeline fireChannelRegistered() {
        return delegate.fireChannelRegistered();
    }

    @Override
    public ChannelPipeline fireChannelUnregistered() {
        return delegate.fireChannelUnregistered();
    }

    @Override
    public ChannelPipeline fireChannelActive() {
        return delegate.fireChannelActive();
    }

    @Override
    public ChannelPipeline fireChannelInactive() {
        return delegate.fireChannelInactive();
    }

    @Override
    public ChannelPipeline fireExceptionCaught(Throwable throwable) {
        return delegate.fireExceptionCaught(throwable);
    }

    @Override
    public ChannelPipeline fireUserEventTriggered(Object o) {
        return delegate.fireUserEventTriggered(o);
    }

    @Override
    public ChannelPipeline fireChannelRead(Object o) {
        return delegate.fireChannelRead(o);
    }

    @Override
    public ChannelPipeline fireChannelReadComplete() {
        return delegate.fireChannelReadComplete();
    }

    @Override
    public ChannelPipeline fireChannelWritabilityChanged() {
        return delegate.fireChannelWritabilityChanged();
    }

    @Override
    public ChannelFuture bind(SocketAddress socketAddress) {
        return delegate.bind(socketAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress) {
        return delegate.connect(socketAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1) {
        return delegate.connect(socketAddress, socketAddress1);
    }

    @Override
    public ChannelFuture disconnect() {
        return delegate.disconnect();
    }

    @Override
    public ChannelFuture close() {
        return delegate.close();
    }

    @Override
    public ChannelFuture deregister() {
        return delegate.deregister();
    }

    @Override
    public ChannelFuture bind(SocketAddress socketAddress, ChannelPromise channelPromise) {
        return delegate.bind(socketAddress, channelPromise);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress, ChannelPromise channelPromise) {
        return delegate.connect(socketAddress, channelPromise);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1, ChannelPromise channelPromise) {
        return delegate.connect(socketAddress, socketAddress1, channelPromise);
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise channelPromise) {
        return delegate.disconnect(channelPromise);
    }

    @Override
    public ChannelFuture close(ChannelPromise channelPromise) {
        return delegate.close(channelPromise);
    }

    @Override
    public ChannelFuture deregister(ChannelPromise channelPromise) {
        return delegate.deregister(channelPromise);
    }

    @Override
    public ChannelPipeline read() {
        return delegate.read();
    }

    @Override
    public ChannelFuture write(Object o) {
        return delegate.write(o);
    }

    @Override
    public ChannelFuture write(Object o, ChannelPromise channelPromise) {
        return delegate.write(o, channelPromise);
    }

    @Override
    public ChannelPipeline flush() {
        return delegate.flush();
    }

    @Override
    public ChannelFuture writeAndFlush(Object o, ChannelPromise channelPromise) {
        return delegate.writeAndFlush(o, channelPromise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object o) {
        return delegate.writeAndFlush(o);
    }

    @Override
    public Iterator<Map.Entry<String, ChannelHandler>> iterator() {
        return delegate.iterator();
    }
}
