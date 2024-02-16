package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.tracker.QueueExecutionWatchdogThread;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.PacketBundleSaver;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.utility.ThreadLocalAccessor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DuplexChannelHandler extends ChannelDuplexHandler implements AbstractPipelineProxy {

    private volatile Player player;
    private final Channel channel;
    private final AtomicBoolean startedInjecting = new AtomicBoolean(false);
    private volatile boolean startedInjectingRound;
    private volatile boolean checkedDecoder;
    private InboundPacketDecoder inboundPacketDecoder;
    private boolean hasRead;
    private boolean hasDecoder;
    private final PacketBundleSaver packetBundleSaver = new PacketBundleSaver();

    private static final Map<UUID, PacketBundleSaver> packetBundleSavers = new HashMap<>();
    private static final ThreadLocalAccessor<UUID> packetSendMarker = new ThreadLocalAccessor<>();
    private static final PacketProcessor packetProcessor = PacketLibrary.get().getPacketProcessor();
    private static final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();

    public DuplexChannelHandler(Channel channel, Player player) {
        this(channel);
        setPlayer(player);
    }

    public DuplexChannelHandler(Channel channel) {
        this.channel = channel;
    }

    private void addPacketBundleSaver() {
        if (player != null)
            packetBundleSavers.put(player.getUniqueId(), packetBundleSaver);
    }

    public void setPlayer(final Player player) {
        this.player = player;
        addPacketBundleSaver();
    }

    public Player getPlayer() {
        return player;
    }

    public static ThreadLocalAccessor<UUID> getPacketSendMarker() {
        return packetSendMarker;
    }

    public static Map<UUID, PacketBundleSaver> getPacketBundleSavers() {
        return packetBundleSavers;
    }

    public boolean hasRead() {
        return hasRead;
    }

    public void startDecodeInjection() {
        if (!player.isOnline()) return;

        ChannelPipeline pipeline = channel.pipeline();
        // waiting for minecraft to add decompress or decode channel handler
        if (pipeline.get("decompress") == null && pipeline.get("decoder") == null) return;

        startedInjectingRound = true;
        startedInjecting.set(true);

        if (CommonChannelInjector.injectDecoder(channel, player) == null && player.isOnline()) {
            Bukkit.getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(),
                    () -> player.kickPlayer("Error occurred when injecting decoder handler for pipeline."));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (HoneyPlugin.get().getLoggerService().isDebug()) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Exception was caught in a channel handler:");
            cause.printStackTrace();
        }
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public synchronized void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
        long readStart = monitorManager.isPacketsMonitored() ? System.nanoTime() : 0;
        hasRead = true;
        // decoder inject for late injection

        if (player != null && !startedInjecting.get()) {
            startDecodeInjection();
            // the injection never started, so we will just return
            if (!startedInjectingRound) return;
        }

        if (startedInjecting.get() && !hasDecoder) {
            if (!(hasDecoder = InboundPacketDecoder.isDecoderRegistered(player)) && !checkedDecoder && !startedInjectingRound) {
                checkedDecoder = true;

                Bukkit.getServer().getScheduler().runTask(HoneyPlugin.get().getPlugin(), () -> {
                    if (!InboundPacketDecoder.isDecoderRegistered(player))
                        player.kickPlayer("Packet decoder was never injected in pipeline.");
                });
            }
            if (!hasDecoder) return;
        }
        startedInjectingRound = false;

        if (inboundPacketDecoder == null && player != null) {
            final AbstractPipelineProxy proxy = HoneyPlugin.get().getPacketLibrary().getInjector().getDecoder(channel);
            if (proxy instanceof InboundPacketDecoder)
                inboundPacketDecoder = (InboundPacketDecoder) proxy;
        }

        // register the handle time between the last handled packet and this one
        if (inboundPacketDecoder != null)
            HoneyPlugin.get().getNettyPacketHandleTracker().registerTime(player, inboundPacketDecoder.getRecentDeltaDecode());

        PacketContainer<?> recent;
        if (player != null && (recent = packetBundleSaver.getRecent(false)) != null && !recent.isHandledByServer()) {
            packetBundleSaver.getRecent(true);
        }

        final PacketContainer<? extends WrappedPacket<?>> packetContainer = packetProcessor.read(ctx.channel(), player, packet, readStart);

        if (packetContainer == null) {
            packetProcessor.errorListeners(player, packet.getClass(), null);
            return;
        }

        if (packetContainer.getState() == PacketState.PLAY)
            QueueExecutionWatchdogThread.clearVerification();

        // check if the packet should be cancelled
        if (packetContainer.isCancelled()) {
            if (player != null && HoneyPlugin.get().getLoggerService().isDebug())
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + packetContainer.getPlayer().getName());
            return;
        }
        if (player != null && packetContainer.getType() != PacketType.Play.Client.KEEP_ALIVE) {
            packetSendMarker.set(player.getUniqueId());
        }

        super.channelRead(ctx, packetContainer.getHandle());
        packetSendMarker.remove();
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
