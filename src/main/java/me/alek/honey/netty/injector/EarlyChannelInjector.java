package me.alek.honey.netty.injector;

import io.netty.channel.*;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.NettyChannelProxy;
import me.alek.honey.netty.api.event.impl.inject.InjectEvent;
import me.alek.honey.netty.utility.reflect.FieldAccessor;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EarlyChannelInjector implements NettyChannelProxy {

    private static final Class<?> networkManagerClass = NMSUtils.getNetworkManagerClass();
    private static final Class<?> minecraftServerClass = NMSUtils.getMinecraftServerClass();
    private static final Class<?> serverConnectionClass = NMSUtils.getServerConnectionClass();
    private static final Class<?> craftServerClass = Reflection.getClass("{obc}.CraftServer");
    private static final FieldAccessor<Object> getMinecraftServer = Reflection.getField(
            craftServerClass, 0, minecraftServerClass);
    private static final FieldAccessor<Object> getServerConnection = Reflection.getField(
            minecraftServerClass, 0, serverConnectionClass);

    private final List<Channel> serverChannels = new ArrayList<>();
    private ChannelInboundHandlerAdapter serverChannelHandler;
    private ChannelInitializer<Channel> beginInitProtocol;
    private ChannelInitializer<Channel> endInitProtocol;
    private List<Object> networkManagers;


    @Override
    public void injectPlayer(Player player, boolean reallyInject) {
        CommonChannelInjector.injectPlayer(player, InjectEvent.InjectType.EARLY, reallyInject);
    }

    @Override
    public void ejectPlayer(Player player) {
        CommonChannelInjector.ejectPlayer(player, InjectEvent.InjectType.EARLY);
    }

    private void createServerChannelHandler() {
        endInitProtocol = new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                try {
                    synchronized (networkManagers) {
                        channel.eventLoop().submit(() -> CommonChannelInjector.injectChannel(channel, InjectEvent.InjectType.EARLY));
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Fejl ved inject channel " + channel);
                }
            }
        };
        beginInitProtocol = new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                channel.pipeline().addLast(endInitProtocol);
            }
        };
        serverChannelHandler = new ChannelInboundHandlerAdapter() {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                if (!(msg instanceof Channel)) {
                    return;
                }
                if (NMSUtils.isFakeChannel((Channel) msg)) {
                    return;
                }
                Channel channel = (Channel) msg;
                channel.pipeline().addFirst(beginInitProtocol);
                ctx.fireChannelRead(msg);
            }
        };
    }

    @Override
    public void inject() {
        Object minecraftServer = getMinecraftServer.get(Bukkit.getServer());
        Object serverConnection = getServerConnection.get(minecraftServer);
        try {
            FieldAccessor<Object> field = Reflection.getParameterizedField(serverConnectionClass, List.class, networkManagerClass);
            networkManagers = (List<Object>) field.get(serverConnection);
        } catch (Exception ex) {
            CommonChannelInjector.inject(InjectEvent.InjectCallback.ERROR, InjectEvent.InjectType.EARLY, InjectEvent.InjectBound.SERVER);

            Bukkit.getLogger().info("Fejl ved server channel injection. Kontakt Alek for fix!");
            Bukkit.getPluginManager().disablePlugin(HoneyPlugin.get());
            return;
        }
        createServerChannelHandler();

        FieldAccessor<List<Object>> accessor = Reflection.getParameterizedField(serverConnectionClass, List.class, ChannelFuture.class);
        List<Object> list = accessor.get(serverConnection);

        if (list.isEmpty()) {
            HoneyPlugin.get().getLogger().info("Ingen server channel fundet! Bruger late injection i stedet for early injection.");

            CommonChannelInjector.inject(InjectEvent.InjectCallback.ERROR, InjectEvent.InjectType.EARLY, InjectEvent.InjectBound.SERVER);
            PacketLibrary.get().setLateProxy();
            return;
        }
        boolean injectedServerChannel = false;
        for (Object item : list) {
            if (!(item instanceof ChannelFuture)) {
                break;
            }
            Channel serverChannel = ((ChannelFuture) item).channel();
            injectServerChannel(serverChannel);
            injectedServerChannel = true;
        }
        if (!injectedServerChannel) {
            HoneyPlugin.get().getLogger().info("Fejl ved inject af server channel! Bruger late injection i stedet for early injection.");

            CommonChannelInjector.inject(InjectEvent.InjectCallback.ERROR, InjectEvent.InjectType.EARLY, InjectEvent.InjectBound.SERVER);
            PacketLibrary.get().setLateProxy();
            return;
        }
        CommonChannelInjector.inject(InjectEvent.InjectCallback.SUCCESS, InjectEvent.InjectType.EARLY, InjectEvent.InjectBound.SERVER);
    }

    private void injectServerChannel(Channel channel) {
        serverChannels.add(channel);
        channel.pipeline().addFirst(serverChannelHandler);
    }

    @Override
    public void eject() {
        if (serverChannelHandler != null) {

            for (Channel serverChannel : serverChannels) {
                final ChannelPipeline pipeline = serverChannel.pipeline();

                serverChannel.eventLoop().execute(() -> {
                    pipeline.remove(serverChannelHandler);
                });
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            ejectPlayer(player);
        }
    }

    public boolean hasInjected(Player player) {
        return CommonChannelInjector.hasInjected(player);
    }

    @Override
    public void writePacket(Player player, Object rawPacket) {
        CommonChannelInjector.writePacket(player, rawPacket);
    }

    @Override
    public void writePacket(Channel channel, Object rawPacket) {
        CommonChannelInjector.writePacket(channel, rawPacket);
    }

    @Override
    public void flushPackets(Player player) {
        CommonChannelInjector.flushPackets(player);
    }

    @Override
    public void flushPackets(Channel channel) {
        CommonChannelInjector.flushPackets(channel);
    }

    @Override
    public void receivePacket(Player player, Object rawPacket) {
        CommonChannelInjector.receivePacket(player, rawPacket);
    }

    @Override
    public void receivePacket(Channel channel, Object rawPacket) {
        CommonChannelInjector.receivePacket(channel, rawPacket);
    }

    @Override
    public DuplexChannelHandler getHandler(Channel channel) {
        return CommonChannelInjector.getHandler(channel);
    }

    @Override
    public InboundPacketDecoder getDecoder(Channel channel) {
        return CommonChannelInjector.getDecoder(channel);
    }
}
