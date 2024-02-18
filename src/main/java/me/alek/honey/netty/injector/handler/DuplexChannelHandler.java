package me.alek.honey.netty.injector.handler;

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
import org.bukkit.entity.Player;

import java.util.*;

public class DuplexChannelHandler extends ChannelDuplexHandler implements UpdatableChannel {

    private static final Map<UUID, PacketBundleSaver> packetBundleSavers = new HashMap<>();
    private static final ThreadLocalAccessor<UUID> packetSendMarker = new ThreadLocalAccessor<>();
    private static final PacketProcessor packetProcessor = PacketLibrary.get().getPacketProcessor();
    private static final MonitorManager monitorManager = HoneyPlugin.get().getMonitorManager();

    private final InboundPacketDecoder decoder;
    private final Channel channel;
    private final PacketBundleSaver packetBundleSaver = new PacketBundleSaver();
    private volatile Player player;

    public DuplexChannelHandler(Channel channel) {
        this.channel = channel;
        decoder = (InboundPacketDecoder) HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(channel);
    }

    public void setPlayer(final Player player) {
        this.player = player;
        packetBundleSavers.put(player.getUniqueId(), packetBundleSaver);
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

        // register the handle time between the last handled packet and this one
        if (decoder != null)
            HoneyPlugin.get().getNettyPacketHandleTracker().registerTime(player, decoder.getRecentDeltaDecode());

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
