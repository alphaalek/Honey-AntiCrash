package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.handler.MinecraftHandlers;
import me.alek.honey.util.reflection.FieldAccessor;
import me.alek.honey.util.reflection.MinecraftReflection;
import me.alek.honey.util.reflection.Reflection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpigotServerInjector {

    public static final String PROTOCOL_SUPPORT_INIT = "SpigotNettyServerChannelHandler#0";
    public static final String PROTOCOL_LIB_INIT = "InjectionChannelInitializer#0";
    public static final String FLOODGATE_INIT = "floodgate-init";
    public static final String UNKNOWN_FORKS_INIT = "MinecraftPipeline#0";
    public static final String PACKET_EVENTS_INIT = "pe-connection-handler";

    private static final FieldAccessor<List<ChannelFuture>> listeningChannelsAccessor;
    private static final FieldAccessor<List<Object>> connectedChannelsAccessor;

    static {
        listeningChannelsAccessor = Reflection.getParameterizedField(MinecraftReflection.getServerConnectionClass(), List.class, ChannelFuture.class);
        connectedChannelsAccessor = Reflection.getParameterizedField(MinecraftReflection.getServerConnectionClass(), List.class, MinecraftReflection.getNetworkManagerClass());
    }

    private List<ChannelFuture> delegatedConnectedChannelsList;
    private boolean injected;
    private final Object serverConnection = MinecraftReflection.getServerConnection();

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
            Channel channel = MinecraftReflection.getChannelFromNetworkManager(connectedChannel);
            if (channel == null) {
                continue;
            }
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().injectChannel(channel);
        }

        this.injected = true;
    }

    private void injectServerChannel(Channel serverChannel) {
        ChannelPipeline pipeline = serverChannel.pipeline();

        if (pipeline.get(HoneyPlugin.HONEY_CONNECTION_HANDLER_NAME) != null) {
            return;
        }

        ChannelHandler initInjector = new BeginInitProtocolInjector();
        String prioritizedHandler = getPrioritizedHandler(pipeline);
        if (prioritizedHandler != null) {
            pipeline.addAfter(prioritizedHandler, HoneyPlugin.HONEY_CONNECTION_HANDLER_NAME, initInjector);
        } else {
            pipeline.addFirst(HoneyPlugin.HONEY_CONNECTION_HANDLER_NAME, initInjector);
        }

        List<Object> connectedChannels = connectedChannelsAccessor.get(this.serverConnection);
        for (Object connectedChannel : connectedChannels) {
            Channel client = MinecraftReflection.getChannelFromNetworkManager(connectedChannel);
            if (client != null && client.isOpen() && client.localAddress().equals(serverChannel.localAddress())) {
                client.close();
            }
        }
    }

    private String getPrioritizedHandler(ChannelPipeline pipeline) {
        List<String> handlersOrdered = new ArrayList<>(pipeline.toMap().keySet());
        String lastPrioritizedHandler = null;
        int lastPrioritizedHandlerIndex = -1;

        // Floodgate
        if (pipeline.get(FLOODGATE_INIT) != null) {
            int index = handlersOrdered.indexOf(FLOODGATE_INIT);
            if (index > lastPrioritizedHandlerIndex) {
                lastPrioritizedHandlerIndex = index;
                lastPrioritizedHandler = FLOODGATE_INIT;
            }
        }
        // ProtocolSupport
        if (pipeline.get(PROTOCOL_SUPPORT_INIT) != null) {
            int index = handlersOrdered.indexOf(PROTOCOL_SUPPORT_INIT);
            if (index > lastPrioritizedHandlerIndex) {
                lastPrioritizedHandlerIndex = index;
                lastPrioritizedHandler = PROTOCOL_SUPPORT_INIT;
            }
        }
        // ProtocolLib
        if (pipeline.get(PROTOCOL_LIB_INIT) != null) {
            int index = handlersOrdered.indexOf(PROTOCOL_LIB_INIT);
            if (index > lastPrioritizedHandlerIndex) {
                lastPrioritizedHandlerIndex = index;
                lastPrioritizedHandler = PROTOCOL_LIB_INIT;
            }
        }
        // Some forks
        if (pipeline.get(UNKNOWN_FORKS_INIT) != null) {
            int index = handlersOrdered.indexOf(UNKNOWN_FORKS_INIT);
            if (index > lastPrioritizedHandlerIndex) {
                lastPrioritizedHandler = UNKNOWN_FORKS_INIT;
            }
        }
        // PacketEvents
        for (String handler : handlersOrdered) {
            if (handler.startsWith(PACKET_EVENTS_INIT)) {
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
            Channel channel = MinecraftReflection.getChannelFromNetworkManager(connectedChannel);
            if (channel == null) {
                continue;
            }
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ejectChannel(channel);
            ChannelCloseInspection.close(channel);
        }

        this.injected = false;
    }

    public boolean isInjected() {
        return this.injected;
    }
}
