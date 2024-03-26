package me.alek.honey.netty.injector.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.NMS;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.netty.utility.Reflection;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.nio.channels.ClosedChannelException;

public class HoneyPacketHandler extends ChannelDuplexHandler implements UpdatableChannel {

    private static final String VIA_VERSION_CLOSED_CHANNEL_EXCEPTION_MESSAGE = "This packet is supposed to be cancelled; If you have debug enabled, you can ignore these";
    private static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> packetSendMarker = new ThreadLocal<>();
    private static final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();

    private volatile HoneyPlayer player;

    public void setPlayer(HoneyPlayer player) {
        this.player = player;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public static ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> getPacketSendMarker() {
        return packetSendMarker;
    }

    public static void onExceptionCaught(Throwable cause) {
        if (HoneyPlugin.get().getLoggerService().isDebug()
                && !(cause instanceof ClosedChannelException)
                && /* ViaVersion */ !cause.getMessage().equals(VIA_VERSION_CLOSED_CHANNEL_EXCEPTION_MESSAGE)) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Exception was caught in a channel handler:");
            cause.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
         onExceptionCaught(cause);
         super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
        long readStart = HoneyPlugin.get().getMonitorManager().isPacketsMonitored() ? System.nanoTime() : 0;

        if (player != null && (!player.isOnline() || player.punished)) {
            return;
        }

        final PacketContainer<? extends WrappedPacket<?>> packetContainer = packetProcessor.read(ctx.channel(), player, packet, readStart, null);

        if (!read0(packetContainer)) {
            return;
        }

        super.channelRead(ctx, packetContainer.getHandle());
        postRead();

        if (packetContainer.getType() == PacketType.Login.Client.LOGIN_START) {
            ReflectStructure<?, ?> loginStartPacketReflectStructure = ReflectStructureCache.acquireStructure(packetContainer.getHandle().getClass());
            String name;
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_18_2)) {
                IStructureModifier<WrappedGameProfile> gameProfileIStructureModifier = loginStartPacketReflectStructure
                        .withType(NMS.getGameProfileClass(), Converters.getGameProfileConverter())
                        .withTarget(packetContainer.getHandle());
                WrappedGameProfile gameProfile = gameProfileIStructureModifier.readField(0);
                name = gameProfile.getName();
            } else {
                IStructureModifier<String> stringReflectStructure = loginStartPacketReflectStructure
                        .asString().withTarget(packetContainer.getHandle());
                name = stringReflectStructure.readField(0);
            }
            NMS.submitChannelForName(name, ctx.channel());
        }
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

        if (packetContainer.getType() == PacketType.Login.Server.LOGIN_SUCCESS
                && PacketInterceptorResolver.isEncoderPositionResolved() && PacketInterceptorResolver.isDecoderPositionResolved()) {
            HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(ctx.channel()).checkRelocation();
        }

        super.write(ctx, packetContainer.getHandle(), promise);
    }

    public <WP extends WrappedPacket<WP>> boolean write0(PacketContainer<WP> packetContainer, ChannelPromise promise) {
        if (packetContainer.isCancelled()) {
            return false;
        }

        if (packetContainer.getPost() != null) {
            promise.addListener((f) -> packetContainer.getPost().call(packetContainer));
        }

        return true;
    }
}
