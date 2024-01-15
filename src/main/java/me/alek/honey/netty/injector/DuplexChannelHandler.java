package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.listeners.PlayerJoinListener;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.utility.ThreadSeperatedQueue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DuplexChannelHandler extends ChannelDuplexHandler implements AbstractPipelineProxy {

    private volatile Player player;
    private final Channel channel;
    private final AtomicBoolean hasStartedInjectingTask = new AtomicBoolean(false);
    private InboundPacketDecoder inboundPacketDecoder;

    private static final ThreadSeperatedQueue<UUID> packetSendQueue = new ThreadSeperatedQueue<>();
    private static final Map<UUID, Queue<PacketContainer<? extends WrappedPacket<?>>>> lastPacketMap = new HashMap<>();

    private static final PacketProcessor packetProcessor = PacketLibrary.get().getPacketProcessor();

    public DuplexChannelHandler(Channel channel, Player player) {
        this.channel = channel;
        this.player = player;
    }

    public DuplexChannelHandler(Channel channel) {
        this.channel = channel;
    }

    public void setPlayer(final Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public static ThreadSeperatedQueue<UUID> getPacketSendQueue() {
        return packetSendQueue;
    }

    public static Map<UUID, Queue<PacketContainer<? extends WrappedPacket<?>>>> getLastPacketMap() {
        return lastPacketMap;
    }

    public void startDecodeInjectorTask(Channel channel) {
        hasStartedInjectingTask.set(true);
        new Thread(() -> {

            while (true) {
                if (player != null && !player.isOnline()) {
                    break;
                }
                ChannelPipeline pipeline = channel.pipeline();

                // waiting for minecraft to add decompress or decode channel handler
                if (pipeline.get("decompress") == null && pipeline.get("decoder") == null) break;
                if (player == null) break;

                CommonChannelInjector.injectDecoder(channel, player);
                return;
            }
        }).start();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
        // decoder inject for late injection
        if (player != null && !hasStartedInjectingTask.get()) {
            startDecodeInjectorTask(this.channel);
        }

        if (inboundPacketDecoder == null && player != null) {
            final AbstractPipelineProxy proxy = HoneyPlugin.get().getPacketLibrary().getInjector().getDecoder(channel);
            if (proxy instanceof InboundPacketDecoder) {
                inboundPacketDecoder = (InboundPacketDecoder) proxy;
            }
        }
        // register the handle time between the last handled packet and this one
        if (inboundPacketDecoder != null) {
            HoneyPlugin.get().getNettyPacketHandleTracker().registerTime(player, inboundPacketDecoder.getRecentDeltaDecode());
        }
        final PacketContainer<? extends WrappedPacket<?>> packetContainer = packetProcessor.read(ctx.channel(), player, packet);

        if (packetContainer == null) {
            packetProcessor.errorListeners(player, packet.getClass(), null);
            return;
        }
        // check if the packet should be cancelled
        if (packetContainer.isCancelled()) {
            if (HoneyPlugin.get().getLoggerService().isDebug()) {
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + packetContainer.getPlayer().getName());
            }
            return;
        }
        if (player != null && packetContainer.getType() != PacketType.Play.Client.KEEP_ALIVE) {
            synchronized (packetSendQueue) {
                packetSendQueue.add(player.getUniqueId());
            }
            lastPacketMap.computeIfAbsent(player.getUniqueId(), (d) -> new LinkedList<>()).add(packetContainer);
        }

        super.channelRead(ctx, packetContainer.getHandle());
        packetProcessor.postRead();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
        if (packet instanceof ByteBuf) {
            super.write(ctx, packet, promise);
            return;
        }
        final PacketContainer<? extends WrappedPacket<?>> packetContainer = packetProcessor.write(ctx.channel(), player, packet);

        if (packetContainer == null) {
            packetProcessor.errorListeners(player, packet.getClass(), null);
            return;
        }
        if (packetContainer.isCancelled()) {
            return;
        }
        if (packetContainer.getPost() != null) {
            promise.addListener((f) -> packetContainer.getPost().run());
        }

        super.write(ctx, packetContainer.getHandle(), promise);
        packetProcessor.postWrite();
    }
}
