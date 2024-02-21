package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.tracker.QueueExecutionWatchdogThread;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.bundle.PacketBundleSaver;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.utility.ThreadLocalAccessor;
import org.bukkit.entity.Player;

import java.util.*;

public class DuplexChannelHandler extends ChannelDuplexHandler implements UpdatableChannel {

    private static final Map<UUID, PacketBundleSaver> packetBundleSavers = new HashMap<>();
    private static final ThreadLocalAccessor<HoneyPlayer> packetSendMarker = new ThreadLocalAccessor<>();
    private static final PacketProcessor packetProcessor = PacketLibrary.get().getPacketProcessor();

    private final InboundPacketDecoder decoder;
    private final PacketBundleSaver packetBundleSaver = new PacketBundleSaver();
    private volatile HoneyPlayer player;

    public DuplexChannelHandler(InboundPacketDecoder decoder) {
        this.decoder = decoder;
    }

    public void setPlayer(HoneyPlayer player) {
        this.player = player;
        packetBundleSavers.put(player.uuid, packetBundleSaver);
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public static ThreadLocalAccessor<HoneyPlayer> getPacketSendMarker() {
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
        long readStart = HoneyPlugin.get().getMonitorManager().isPacketsMonitored() ? System.nanoTime() : 0;

        // register the handle time between the last handled packet and this one
        if (player != null)
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
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + packetContainer.getPlayer().name);
            return;
        }
        if (player != null && packetContainer.getType() != PacketType.Play.Client.KEEP_ALIVE) {
            packetSendMarker.set(player);
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
