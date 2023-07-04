package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.event.impl.inject.InjectEvent;
import me.alek.honey.netty.api.event.impl.inject.PlayerEjectEvent;
import me.alek.honey.netty.api.event.impl.inject.PlayerInjectEvent;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class CommonChannelInjector {

    private static final List<UUID> injectedPlayers = new ArrayList<>();

    public static void inject(InjectEvent.InjectCallback callback, InjectEvent.InjectType type, InjectEvent.InjectBound bound) {
        final InjectEvent event = new InjectEvent(type, callback, bound);
        PacketLibrary.get().callSyncEvent(event, false);

        for (Player player : Bukkit.getOnlinePlayers()) {
            injectPlayer(player, type, true);
        }
    }

    public static void injectPipeline(ChannelHandler handler, String minecraftName, String name, Channel channel) {
        final ChannelPipeline pipeline = channel.pipeline();

        if (pipeline.get(name) != null) {
            channel.eventLoop().execute(() -> {
                pipeline.remove(name);
            });
        }
        channel.eventLoop().execute(() -> {
            if (minecraftName.equals("packet_handler")) {
                pipeline.addBefore(minecraftName, name, handler);
            }
            else if (minecraftName.equals("decompress")) {
                pipeline.addAfter(minecraftName, name, handler);
            }
        });
    }

    public static ChannelHandler injectHandler(Channel channel, Player player) {
        final DuplexChannelHandler handler = new DuplexChannelHandler(player);
        final String handlerName = PacketLibrary.get().getHandlerName();

        injectPipeline(handler, "packet_handler", handlerName, channel);
        return handler;
    }

    public static ChannelHandler injectDecoder(Channel channel, Player player) {
        final InboundPacketDecoder handler = new InboundPacketDecoder(player);
        final String handlerName = PacketLibrary.get().getDecoderName();

        injectPipeline(handler, "decompress", handlerName, channel);
        return handler;
    }

    public static ChannelHandler injectChannel(Channel channel, InjectEvent.InjectType injectType) {
        UUID uuid = NMSUtils.getUUIDForChannel(channel);
        if (uuid == null || Bukkit.getPlayer(uuid) == null) {
            return injectHandler(channel, null);
        }
        else {
            return injectPlayer(Objects.requireNonNull(Bukkit.getPlayer(uuid)), injectType, true);
        }
    }

    public static ChannelHandler injectPlayer(Player player, InjectEvent.InjectType injectType, boolean reallyInject) {
        if (injectedPlayers.contains(player.getUniqueId())) {
            return getHandler(NMSUtils.getChannel(player));
        }
        injectedPlayers.add(player.getUniqueId());

        final Channel channel = NMSUtils.getChannel(player);
        final PlayerInjectEvent event;

        ChannelHandler handler = null;
        if (channel == null) {
            event = new PlayerInjectEvent(player, injectType, InjectEvent.InjectCallback.ERROR);
        }
        else {
            event = new PlayerInjectEvent(player, injectType, InjectEvent.InjectCallback.SUCCESS);
            if (reallyInject) {
                handler = injectHandler(channel, player);
            }
            else {
                handler = getHandler(channel);
            }
        }
        PacketLibrary.get().callSyncEvent(event, false);
        return handler;
    }

    public static void ejectPlayer(Player player, InjectEvent.InjectType injectType) {
        if (!injectedPlayers.contains(player.getUniqueId())) {
            return;
        }
        injectedPlayers.remove(player.getUniqueId());

        final Channel channel = NMSUtils.getChannel(player);
        final PlayerEjectEvent event;
        if (channel == null) {
            event = new PlayerEjectEvent(player, injectType, InjectEvent.InjectCallback.ERROR);
        }
        else {
            event = new PlayerEjectEvent(player, injectType, InjectEvent.InjectCallback.SUCCESS);

            final ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(PacketLibrary.get().getHandlerName()) != null) {

                try {
                    channel.eventLoop().execute(() -> {
                        pipeline.remove(PacketLibrary.get().getHandlerName());
                    });
                } catch (NoSuchElementException ex) {
                }
            }
        }
        PacketLibrary.get().callSyncEvent(event, false);
    }

    public static boolean hasInjected(Player player) {
        final Channel channel = NMSUtils.getChannel(player);
        if (channel == null) {
            return false;
        }
        return injectedPlayers.contains(player.getUniqueId()) || channel.pipeline().get(PacketLibrary.get().getHandlerName()) != null;
    }

    public static void writePacket(Player player, Object rawPacket) {
        writePacket(NMSUtils.getChannel(player), rawPacket);
    }

    public static void writePacket(Channel channel, Object rawPacket) {
        if (NMSUtils.isFakeChannel(channel)) {
            return;
        }
        channel.writeAndFlush(rawPacket);
    }

    public static void flushPackets(Player player) {
        flushPackets(NMSUtils.getChannel(player));
    }

    public static void flushPackets(Channel channel) {
        if (NMSUtils.isFakeChannel(channel)) {
            return;
        }
        channel.flush();
    }

    public static void receivePacket(Player player, Object rawPacket) {
        receivePacket(NMSUtils.getChannel(player), rawPacket);
    }

    public static void receivePacket(Channel channel, Object rawPacket) {
        if (NMSUtils.isFakeChannel(channel)) {
            return;
        }
        final ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.context("encoder") == null) {
            return;
        }
        pipeline.context("encoder").fireChannelRead(rawPacket);
    }

    private static ChannelHandler getHandler(Channel channel, boolean recursiveLimit) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(PacketLibrary.get().getHandlerName()) == null) {
            if (recursiveLimit) {
                return null;
            }
            return CommonChannelInjector.injectChannel(channel, InjectEvent.InjectType.LATE);
        }
        else {
            ChannelHandler channelHandler = pipeline.get(PacketLibrary.get().getHandlerName());
            if (channel instanceof DuplexChannelHandler && !(channelHandler instanceof DuplexChannelHandler)
                    ||  channel instanceof InboundPacketDecoder && !(channelHandler instanceof InboundPacketDecoder)) {
                return CommonChannelInjector.injectChannel(channel, InjectEvent.InjectType.LATE);
            }
            return channelHandler;
        }
    }

    public static DuplexChannelHandler getHandler(Channel channel) {
        return (DuplexChannelHandler) channel.pipeline().get(PacketLibrary.get().getHandlerName());
    }

    public static InboundPacketDecoder getDecoder(Channel channel) {
        return (InboundPacketDecoder) channel.pipeline().get(PacketLibrary.get().getDecoderName());
    }

}
