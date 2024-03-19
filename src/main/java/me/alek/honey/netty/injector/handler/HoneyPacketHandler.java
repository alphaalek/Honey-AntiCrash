package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.NMS;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.nio.channels.ClosedChannelException;
import java.util.UUID;

public class HoneyPacketHandler extends ChannelDuplexHandler implements UpdatableChannel {

    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = new ThreadLocal<>();
    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();

    private volatile HoneyPlayer player;
    private Object networkManager;

    public void setPlayer(HoneyPlayer player) {
        this.player = player;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public Object getNetworkManager() {
        return networkManager;
    }

    public static ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> getPacketSendMarker() {
        return packetSendMarker;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (HoneyPlugin.get().getLoggerService().isDebug()
                && !(cause instanceof ClosedChannelException)) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Exception was caught in a channel handler:");
            cause.printStackTrace();
        }

        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
        long readStart = HoneyPlugin.get().getMonitorManager().isPacketsMonitored() ? System.nanoTime() : 0;

        final PacketContainer<? extends WrappedPacket<?>> packetContainer = packetProcessor.read(ctx.channel(), player, packet, readStart, null);
        if (packetContainer == null) {
            packetProcessor.errorListeners(player, packet.getClass(), null);
            return;
        }

        if (networkManager == null)
            networkManager = HoneyPlugin.get().getPacketLibrary().getServerInjector().getNetworkManagerFor(ctx.channel());

        if (!read0(packetContainer)) {
            return;
        }

        super.channelRead(ctx, packetContainer.getHandle());
        postRead();
    }

    public synchronized boolean read0(PacketContainer<? extends WrappedPacket<?>> packetContainer) {
        if (packetContainer.isCancelled()) {
            if (player != null && HoneyPlugin.get().getLoggerService().isDebug())
                HoneyPlugin.get().getPlugin().getLogger().info("DEBUG: packet cancelled: " + packetContainer.getType() + ", " + packetContainer.getPlayer().name);
            return false;
        }

        if (player != null && packetContainer.getPlayer() != null && packetContainer.getType() != PacketType.Play.Client.KEEP_ALIVE) {
            packetSendMarker.set(packetContainer);
        }

        return true;
    }

    public void postRead() {
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

        if (!write0(packetContainer, promise)) {
            return;
        }

        super.write(ctx, packetContainer.getHandle(), promise);
        packetProcessor.postWrite();

        if (packetContainer.getType() == PacketType.Login.Server.SET_COMPRESSION && networkManager != null
                && PacketInterceptorResolver.isEncoderPositionResolved() && PacketInterceptorResolver.isDecoderPositionResolved()) {
            HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(ctx.channel()).checkRelocation();
        }
        if (packetContainer.getType() == PacketType.Login.Client.LOGIN_START) {
            IStructureModifier<UUID> gameProfileReflectStructure = ReflectStructureCache.acquireStructure(packetContainer.getHandle().getClass())
                    .withType(UUID.class)
                    .withTarget(packetContainer.getHandle());
            UUID gameProfileUUID = gameProfileReflectStructure.readField(0);

            NMS.submitChannelForUUID(gameProfileUUID, ctx.channel());
        }
    }

    public boolean write0(PacketContainer<? extends WrappedPacket<?>> packetContainer, ChannelPromise promise) {
        if (packetContainer.isCancelled()) {
            return false;
        }

        if (packetContainer.getPost() != null) {
            promise.addListener((f) -> packetContainer.getPost().run());
        }

        return true;
    }
}
