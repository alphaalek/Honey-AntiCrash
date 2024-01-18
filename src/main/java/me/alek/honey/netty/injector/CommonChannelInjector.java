package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.event.impl.inject.InjectEvent;
import me.alek.honey.netty.api.event.impl.inject.PlayerEjectEvent;
import me.alek.honey.netty.api.event.impl.inject.PlayerInjectEvent;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommonChannelInjector {

    private static final List<UUID> injectedPlayers = new ArrayList<>();
    private static final CountDownLatch injectedLatch = new CountDownLatch(1);

    public static CountDownLatch getInjectedLatch() {
        return injectedLatch;
    }

    public static void inject(InjectEvent.InjectCallback callback, InjectEvent.InjectType type, InjectEvent.InjectBound bound) {
        final InjectEvent event = new InjectEvent(type, callback, bound);
        PacketLibrary.get().callSyncEvent(event, false);

        for (Player player : Bukkit.getOnlinePlayers()) {
            injectPlayer(player, type, true);
        }
    }

    private enum PipelineInjectionRelative {
        BEFORE, AFTER
    }

    public static void injectPipeline(ChannelHandler handler, String nmsName, String name, PipelineInjectionRelative relative, Channel channel) {
        final ChannelPipeline pipeline = channel.pipeline();

        // ignored already injected pipelines
        if (pipeline.get(name) != null) return;

        channel.eventLoop().execute(() -> {
            if (nmsName.equals("decompress")) {
                if (pipeline.get(nmsName) == null) {

                    // no decompress in pipeline, moving decoder further to real decoder
                    pipeline.addBefore("decoder", name, handler);
                    return;
                }
                pipeline.addAfter(nmsName, name, handler);
            } else {
                if (relative == PipelineInjectionRelative.BEFORE) pipeline.addBefore(nmsName, name, handler);
                if (relative == PipelineInjectionRelative.AFTER) pipeline.addAfter(nmsName, name, handler);
            }
        });
    }

    private static void saveInjection(Player player) {
        if (player == null)
            return;

        UUID uuid = player.getUniqueId();
        if (!injectedPlayers.contains(uuid)) injectedPlayers.add(uuid);
    }

    public static ChannelHandler injectHandler(Channel channel, Player player) {
        final DuplexChannelHandler handler = new DuplexChannelHandler(channel, player);
        final String handlerName = PacketLibrary.get().getHandlerName();

        saveInjection(player);

        injectPipeline(handler, "packet_handler", handlerName, PipelineInjectionRelative.BEFORE, channel);

        // done injecting, so notify every waiting thread
        injectedLatch.countDown();

        return handler;
    }

    public static ChannelHandler injectDecoder(Channel channel, Player player) {
        final InboundPacketDecoder handler = new InboundPacketDecoder(player);
        final String handlerName = PacketLibrary.get().getDecoderName();

        injectPipeline(handler, "decompress", handlerName, PipelineInjectionRelative.BEFORE, channel);

        return handler;
    }

    public static ChannelHandler injectChannel(Channel channel, InjectEvent.InjectType injectType) {
        UUID uuid = NMSUtils.getUUIDForChannel(channel);

        if (uuid == null || Bukkit.getPlayer(uuid) == null) {
            return injectHandler(channel, null);
        }
        else return injectPlayer(Objects.requireNonNull(Bukkit.getPlayer(uuid)), injectType, true);
    }

    public static ChannelHandler injectPlayer(Player player, InjectEvent.InjectType injectType, boolean reallyInject) {
        if (injectedPlayers.contains(player.getUniqueId())) {
            return getHandler(NMSUtils.getChannel(player));
        }
        injectedPlayers.add(player.getUniqueId());

        final Channel channel = NMSUtils.getChannel(player);

        final PlayerInjectEvent event;

        ChannelHandler handler = null;
        if (channel == null)
            event = new PlayerInjectEvent(player, injectType, InjectEvent.InjectCallback.ERROR);
        else {
            event = new PlayerInjectEvent(player, injectType, InjectEvent.InjectCallback.SUCCESS);
            if (reallyInject)
                handler = injectHandler(channel, player);
            else
                handler = getHandler(channel);

        }
        PacketLibrary.get().callSyncEvent(event, false);

        return handler;
    }

    public static void ejectPlayers(InjectEvent.InjectType type) {
        CountDownLatch ejectingAllPlayersLatch = new CountDownLatch(Bukkit.getOnlinePlayers().size());
        for (Player player : Bukkit.getOnlinePlayers()) {
            ejectPlayer(player, type, ejectingAllPlayersLatch);
        }
        try {
            ejectingAllPlayersLatch.await();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void ejectPlayer(Player player, InjectEvent.InjectType injectType, CountDownLatch ejectingLatch) {
        injectedPlayers.remove(player.getUniqueId());

        final Channel channel = NMSUtils.getChannel(player);
        final PlayerEjectEvent event;
        if (channel == null) {
            event = new PlayerEjectEvent(player, injectType, InjectEvent.InjectCallback.ERROR);
            if (ejectingLatch != null) ejectingLatch.countDown();
        }
        else {
            event = new PlayerEjectEvent(player, injectType, InjectEvent.InjectCallback.SUCCESS);

            final ChannelPipeline pipeline = channel.pipeline();
            AtomicBoolean hasCountdown = new AtomicBoolean();
            try {
                channel.eventLoop().execute(() -> {
                    if (ejectingLatch != null) ejectingLatch.countDown();
                    hasCountdown.set(true);

                    if (pipeline.get(PacketLibrary.get().getHandlerName()) != null)  {
                        pipeline.remove(PacketLibrary.get().getHandlerName());
                    }
                    if (pipeline.get(PacketLibrary.get().getDecoderName()) != null)  {
                        pipeline.remove(PacketLibrary.get().getDecoderName());
                    }
                });
            } catch (NoSuchElementException ex) {
                if (!hasCountdown.get() && ejectingLatch != null)
                    ejectingLatch.countDown();
            }
        }
        PacketLibrary.get().callSyncEvent(event, false);
    }

    public static void ejectPlayer(Player player, InjectEvent.InjectType injectType) {
        ejectPlayer(player, injectType, null);
    }

    public static boolean hasInjected(Player player) {
        final Channel channel = NMSUtils.getChannel(player);
        if (channel == null) {
            return false;
        }
        return injectedPlayers.contains(player.getUniqueId()) || channel.pipeline().get(PacketLibrary.get().getHandlerName()) != null;
    }

    public static <WP extends WrappedPacket<WP>> void writePacket(Player player, WrappedPacket<WP> packet) {
        writePacket(player, packet.getHandle());
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

    public static <WP extends WrappedPacket<WP>> void writePacket(Channel channel, WrappedPacket<WP> packet) {
        writePacket(channel, packet.getHandle());
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

    private static ChannelHandler getHandler0(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(PacketLibrary.get().getHandlerName()) == null) {
            return CommonChannelInjector.injectChannel(channel, InjectEvent.InjectType.LATE);
        }
        else {
            ChannelHandler channelHandler = pipeline.get(PacketLibrary.get().getHandlerName());
            if (!(channelHandler instanceof InboundPacketDecoder || channelHandler instanceof DuplexChannelHandler)) {
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
