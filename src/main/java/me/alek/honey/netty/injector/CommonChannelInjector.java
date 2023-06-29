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

    private static void injectPipeline(Map<String, ChannelHandler> handlers, ChannelHandler handler, String minecraftName, String name, Channel channel) {
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
            else if (minecraftName.equals("decoder")) {

                if (pipeline.get("decompress") != null) {
                    pipeline.addAfter("decompress", name, handler);
                }
                else if (pipeline.get("splitter") != null) {
                    pipeline.addAfter("splitter", name, handler);
                }
            }
        });
        handlers.put(name, handler);
    }

    private static Map<String, ChannelHandler> injectPipeline(Channel channel) {
        final Map<String, ChannelHandler> handlers = new HashMap<>();

        final PlayerChannelDuplexHandler handler = new PlayerChannelDuplexHandler();
        final PlayerPacketDecoder decoder = new PlayerPacketDecoder();

        final String handlerName = PacketLibrary.get().getHandlerName();
        final String decoderName = PacketLibrary.get().getDecoderName();

        injectPipeline(handlers, handler, "packet_handler", handlerName, channel);
        injectPipeline(handlers, decoder, "decoder", decoderName, channel);

        return handlers;
    }

    public static Map<String, ChannelHandler> injectChannel(Channel channel, InjectEvent.InjectType injectType) {
        UUID uuid = NMSUtils.getUUIDForChannel(channel);
        if (uuid == null || Bukkit.getPlayer(uuid) == null) {
            return injectPipeline(channel);
        }
        else {
            return injectPlayer(Bukkit.getPlayer(uuid), injectType, true);
        }
    }

    public static Map<String, ChannelHandler> injectPlayer(Player player, InjectEvent.InjectType injectType, boolean reallyInject) {
        if (injectedPlayers.contains(player.getUniqueId())) {
            return getChannelHandlers(NMSUtils.getChannel(player));
        }
        injectedPlayers.add(player.getUniqueId());

        final Channel channel = NMSUtils.getChannel(player);
        final PlayerInjectEvent event;

        Map<String, ChannelHandler> handlers = new HashMap<>();
        if (channel == null) {
            event = new PlayerInjectEvent(player, injectType, InjectEvent.InjectCallback.ERROR);
        }
        else {
            event = new PlayerInjectEvent(player, injectType, InjectEvent.InjectCallback.SUCCESS);
            if (reallyInject) {
                handlers = injectPipeline(channel);
            }
            else {
                handlers = getChannelHandlers(channel);
            }
        }
        PacketLibrary.get().callSyncEvent(event, false);
        return handlers;
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

    public static Map<String, ChannelHandler> getChannelHandlers(Channel channel) {
        final Map<String, ChannelHandler> handlers = new HashMap<>();

        handlers.put(PacketLibrary.get().getHandlerName(), getHandler(channel));
        handlers.put(PacketLibrary.get().getDecoderName(), getDecoder(channel));

        return handlers;
    }

    public static ChannelHandler getChannelHandler(Channel channel, String name, boolean recursiveLimit) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(name) == null) {
            if (recursiveLimit) {
                return null;
            }
            return CommonChannelInjector.injectChannel(channel, InjectEvent.InjectType.LATE).get(name);
        }
        else {
            ChannelHandler channelHandler = pipeline.get(name);
            if (channel instanceof PlayerChannelDuplexHandler && !(channelHandler instanceof PlayerChannelDuplexHandler)
            ||  channel instanceof PlayerPacketDecoder && !(channelHandler instanceof PlayerPacketDecoder)) {
                return CommonChannelInjector.injectChannel(channel, InjectEvent.InjectType.LATE).get(name);
            }
            return channelHandler;
        }
    }

    public static PlayerChannelDuplexHandler getHandler(Channel channel, boolean recursiveLimit) {
        return (PlayerChannelDuplexHandler) getChannelHandler(channel, PacketLibrary.get().getHandlerName(), recursiveLimit);
    }

    public static PlayerChannelDuplexHandler getHandler(Channel channel) {
        return getHandler(channel, false);
    }

    public static PlayerPacketDecoder getDecoder(Channel channel) {
        return (PlayerPacketDecoder) getChannelHandler(channel, PacketLibrary.get().getDecoderName(), false);
    }
}
