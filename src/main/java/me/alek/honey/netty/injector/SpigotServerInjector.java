package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.handler.MinecraftHandlers;
import me.alek.honey.util.reflection.FieldAccessor;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpigotServerInjector {

    private static final FieldAccessor<List<ChannelFuture>> listeningChannelsAccessor;
    private static final FieldAccessor<List<Object>> connectedChannelsAccessor;

    static {
        listeningChannelsAccessor = Reflection.getParameterizedField(NMS.getServerConnectionClass(), List.class, ChannelFuture.class);
        connectedChannelsAccessor = Reflection.getParameterizedField(NMS.getServerConnectionClass(), List.class, NMS.getNetworkManagerClass());
    }


    private List<ChannelFuture> delegatedConnectedChannelsList;
    private boolean injected;
    private final Object serverConnection = NMS.getServerConnection();

    public boolean hasAnyListeningChannel() {
        return !listeningChannelsAccessor.get(this.serverConnection).isEmpty();
    }

    public synchronized void inject() {
        if (this.injected) {
            return;
        }

        this.delegatedConnectedChannelsList = listeningChannelsAccessor.get(this.serverConnection);
        synchronized (this.delegatedConnectedChannelsList) {
            for (ChannelFuture listeningChannel : this.delegatedConnectedChannelsList) {
                injectServerChannel(listeningChannel.channel());
            }
            listeningChannelsAccessor.set(this.serverConnection, new ArrayList<ChannelFuture>(this.delegatedConnectedChannelsList) {

                @Override
                public boolean add(ChannelFuture future) {
                    if (injected) {
                        injectServerChannel(future.channel());
                    }
                    return delegatedConnectedChannelsList.add(future);
                }
            });
        }

        List<Object> connectedChannels = connectedChannelsAccessor.get(this.serverConnection);
        for (Object connectedChannel : connectedChannels) {
            Channel channel = NMS.getChannelFromNetworkManager(connectedChannel);
            if (channel == null) {
                continue;
            }
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().injectChannel(channel);
        }

        this.injected = true;
    }

    private void injectServerChannel(Channel serverChannel) {
        ChannelPipeline pipeline = serverChannel.pipeline();

        String connectorName = HoneyPlugin.get().getPacketLibrary().getConnectorName();
        if (pipeline.get(connectorName) != null) {
            pipeline.remove(connectorName);
        }

        String prioritizedHandler = getPrioritizedHandler(pipeline);
        if (prioritizedHandler != null) {
            pipeline.addAfter(prioritizedHandler, connectorName, new BeginInitProtocolInjector());
        } else {
            pipeline.addFirst(connectorName, new BeginInitProtocolInjector());
        }

        List<Object> connectedChannels = connectedChannelsAccessor.get(this.serverConnection);
        for (Object connectedChannel : connectedChannels) {
            Channel client = NMS.getChannelFromNetworkManager(connectedChannel);
            if (client != null && client.isOpen() && client.localAddress().equals(serverChannel.localAddress())) {
                client.close();
            }
        }
    }

    private String getPrioritizedHandler(ChannelPipeline pipeline) {
        List<String> handlersOrdered = new ArrayList<>(pipeline.toMap().keySet());
        String lastPrioritizedHandler = null;
        int lastPrioritizedHandlerIndex = -1;

        // ProtocolSupport
        if (pipeline.get("SpigotNettyServerChannelHandler#0") != null) {
            int index = handlersOrdered.indexOf("SpigotNettyServerChannelHandler#0");
            if (index > lastPrioritizedHandlerIndex) {
                lastPrioritizedHandlerIndex = index;
                lastPrioritizedHandler = "SpigotNettyServerChannelHandler0";
            }
        }
        // Floodgate
        if (pipeline.get("floodgate-init") != null) {
            int index = handlersOrdered.indexOf("floodgate-init");
            if (index > lastPrioritizedHandlerIndex) {
                lastPrioritizedHandlerIndex = index;
                lastPrioritizedHandler = "floodgate-init";
            }
        }
        // Some forks
        if (pipeline.get("MinecraftPipeline#0") != null) {
            int index = handlersOrdered.indexOf("MinecraftPipeline#0");
            if (index > lastPrioritizedHandlerIndex) {
                lastPrioritizedHandler = "MinecraftPipeline#0";
            }
        }
        // PacketEvents
        for (String handler : handlersOrdered) {
            if (handler.startsWith("pe-connection-handler")) {
                int index = handlersOrdered.indexOf(handler);
                if (index > lastPrioritizedHandlerIndex) {
                    lastPrioritizedHandler = handler;
                }
            }
        }

        return lastPrioritizedHandler;
    }

    public Object getNetworkManagerFor(Channel channel) {
        return channel.pipeline().get(MinecraftHandlers.CONNECTION);
    }

    public synchronized void eject() {
        List<ChannelFuture> listeningChannels = listeningChannelsAccessor.get(this.serverConnection);
        for (ChannelFuture channelFuture : listeningChannels) {
            for (Map.Entry<String, ChannelHandler> handler : channelFuture.channel().pipeline()) {
                if (handler.getValue() instanceof BeginInitProtocolInjector) {
                    channelFuture.channel().pipeline().remove(handler.getValue());
                }
            }
        }

        List<Object> connectedChannels = connectedChannelsAccessor.get(this.serverConnection);
        for (Object connectedChannel : connectedChannels) {
            Channel channel = NMS.getChannelFromNetworkManager(connectedChannel);
            if (channel == null) {
                continue;
            }
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ejectChannel(channel);
        }

        this.injected = false;
    }

    public boolean isInjected() {
        return this.injected;
    }
}
