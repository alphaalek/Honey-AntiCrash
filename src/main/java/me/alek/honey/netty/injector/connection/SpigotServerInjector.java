package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.NMSUtils;

import java.util.*;

public class SpigotServerInjector {

    private boolean injected;
    private int channelFuturesFieldIndex = -1;
    private IStructureModifier<List<?>> serverConnectionListAccessor;
    private List<ChannelFuture> channelFutures;
    public static List<Object> networkManagers;

    public boolean isServerBound() {
        getChannelFutures();
        
        return channelFutures != null;
    }

    private <T> List<T> getNonEmptyServerConnectionList(Class<T> clazz) {
        if (serverConnectionListAccessor == null) {
            Object serverConnection = NMSUtils.getServerConnection();

            serverConnectionListAccessor = ReflectStructureCache.acquireStructure(NMSUtils.getServerConnectionClass())
                    .<List<?>>withWeakType(List.class).withTarget(serverConnection);
        }
        if (serverConnectionListAccessor == null) return null;

        for (List<?> list : serverConnectionListAccessor.readFieldValues()) {
            if (!list.isEmpty() && clazz.isAssignableFrom(list.get(0).getClass())) {
                return (List<T>) list;
            }
        }
        return null;
    }
    
    private void getChannelFutures() {
        List<ChannelFuture> channelFutures = getNonEmptyServerConnectionList(ChannelFuture.class);

        if (channelFutures == null) return;

        int fieldIndex = channelFuturesFieldIndex;
        if (fieldIndex == -1) {

            boolean foundChannelFutures = false;
            for (List<?> list : serverConnectionListAccessor.readFieldValues()) {
                if (!foundChannelFutures) fieldIndex++;

                if (list.equals(channelFutures))
                    foundChannelFutures = true;
                else
                    networkManagers = (List<Object>) list;
            }
            channelFuturesFieldIndex = fieldIndex;
        }

        serverConnectionListAccessor.writeField(fieldIndex, this.channelFutures = new ArrayList<ChannelFuture>() {

            @Override
            public boolean add(ChannelFuture future) {
                injectServerChannel(future.channel());
                return super.add(future);
            }
        });
        // we can't use ArrayList#addAll, because then the overridden method will not be run
        for (ChannelFuture channelFuture : channelFutures) {
            this.channelFutures.add(channelFuture);
        }
    }

    private synchronized void injectServerChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addFirst(new BeginInitProtocolInjector());

        synchronized (networkManagers) {
            for (Object networkManager : networkManagers) {

                Channel networkManagerChannel = NMSUtils.getChannelFromNetworkManager(networkManager);
                if (networkManagerChannel == null || !networkManagerChannel.isOpen()) continue;

                if (networkManagerChannel.localAddress().equals(channel.localAddress())) {
                    networkManagerChannel.close();
                }
            }
        }
    }

    public synchronized void inject() {
        if (injected) return;

        if (channelFutures == null) {
            getChannelFutures();
            if (channelFutures == null) {
                return;
            }
        }

        synchronized (networkManagers) {
            for (Object networkManager : networkManagers) {

                Channel channel = NMSUtils.getChannelFromNetworkManager(networkManager);
                if (channel == null) continue;

                PacketLibrary.get().getPipelineInjector().injectChannel(channel);
            }
        }
        injected = true;
    }

    public synchronized void eject() {
        synchronized (channelFutures) {
            serverConnectionListAccessor.writeField(channelFuturesFieldIndex, Collections.synchronizedList(new ArrayList<>(channelFutures)));

            for (ChannelFuture channelFuture : channelFutures) {
                Iterator<Map.Entry<String, ChannelHandler>> handlerIterator = channelFuture.channel().pipeline().iterator();

                while (handlerIterator.hasNext()) {
                    Map.Entry<String, ChannelHandler> handler = handlerIterator.next();

                    if (handler.getValue() instanceof BeginInitProtocolInjector) {
                        handlerIterator.remove();
                    }
                }
            }
        }

        synchronized (networkManagers) {
            for (Object networkManager : networkManagers) {

                Channel channel = NMSUtils.getChannelFromNetworkManager(networkManager);
                if (channel == null) continue;

                PacketLibrary.get().getPipelineInjector().ejectChannel(channel);
            }
        }
        injected = false;
    }

    public boolean isInjected() {
        return injected;
    }
}
