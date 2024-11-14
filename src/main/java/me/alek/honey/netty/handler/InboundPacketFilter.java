package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.UnsafeChannelCloser;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.util.reflection.Reflection;
import me.alek.honey.validation.impl.FloodValidations;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.logging.Level;

@ChannelHandler.Sharable
public class InboundPacketFilter extends MessageToMessageDecoder<ByteBuf> {

    private static final String CONNECTION_EVENT_CLASS_NAME = "io.papermc.paper.network.ConnectionEvent";
    private static final String COMPRESSION_THRESHOLD_SET_FIELD_NAME = "COMPRESSION_THRESHOLD_SET";
    private static final Object PAPER_COMPRESSION_ENABLED_EVENT = getPaperCompressionEnabledEvent();

    public static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> MARK = new ThreadLocal<>();

    private static Object getPaperCompressionEnabledEvent() {
        try {
            Class<?> eventClass = Reflection.getClass(CONNECTION_EVENT_CLASS_NAME);
            return Reflection.getField(eventClass, COMPRESSION_THRESHOLD_SET_FIELD_NAME).get(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private final HoneyPlayer player;
    private final HoneyPacketHandler packetHandler;
    private boolean hasCalledChannelInactive;

    public InboundPacketFilter(HoneyPacketHandler handler) {
        this.packetHandler = handler;
        this.player = handler.getPlayer();
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.fireChannelActive();

        if (ctx.channel().remoteAddress() == null) {
            // null ping
            ctx.close();
            return;
        }

        try {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().injectChannelProxy(ctx.channel());
        } catch (MissingHandlerException ex) {
            ctx.close();
        }
        HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(ctx.channel());

        try {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(ctx.channel()).setProtocolState(PacketBound.SERVER, PacketState.HANDSHAKE);
        } catch (MissingHandlerException ex) {
            ctx.close();
        }
    }

    @Override
    public synchronized void channelInactive(ChannelHandlerContext ctx) {
        ctx.fireChannelInactive();

        if (!hasCalledChannelInactive) {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().decrementTaskSupplierCount(ctx.channel());
        }
        hasCalledChannelInactive = true;

        HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ejectChannel(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
        ctx.fireUserEventTriggered(event);

        if (event.equals(PAPER_COMPRESSION_ENABLED_EVENT)) {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(ctx.channel());
        }
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) {
        if (!player.isStreamActive()) {
            return;
        }

        if (!buf.isReadable() || this.packetHandler.isResolving(PacketBound.SERVER)) {
            ctx.fireChannelRead(buf);
            return;
        }

        this.packetHandler.runDelayedTasks(PacketBound.SERVER);

        PacketContainer<? extends WrappedPacket<?>> prevMarked = MARK.get();
        // check for too early dropped packet in the previous eventloop
        // if the filter mark has a value, it means that a channel handler between
        // the filter and decode preparation dropped this packet
        if (prevMarked != null && !PacketTypes.isUnknown(prevMarked.getType())) {
            FloodValidations.PACKET_DROP_PIPELINE.doStream(
                    this.player,
                    (validation, context) -> validation.validate(this.player, 1, context),
                    prevMarked);

            prevMarked.releaseIfAutoReleasable();
        }

        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        PacketContainer<? extends WrappedPacket<?>> packet = null;
        try {
            // initialize packet container
            PacketType type = this.packetHandler.getPacketType(serializer, false, PacketBound.SERVER, true);
            packet = new InternalPacketContainer<>(PacketContainer.PipelineState.INITIAL_BYTE, serializer, this.player, type, null);
            MARK.set(packet);

            // call listeners
            if (!player.bypassEverything()) {
                packet.getEventloopStateTimings().startTimings(LifecycleState.HONEY_HANDLE);
                HoneyPlugin.get().getPacketLibrary().getPacketProcessor().callInitialBytePacketProcessListeners(this.player, packet);
                packet.getEventloopStateTimings().stopTimings();
            }

            if (!packetHandler.isCancelled(packet)) {
                list.add(serializer
                        .readerIndex(0)
                        .retain(Math.max(serializer.refCnt(), 1) + 1));
            } else {
                list.add(Unpooled.buffer());
                packet.releaseIfAutoReleasable();
            }

            // start in-between timings - stopped in decode preparation handler
            packet.getEventloopStateTimings().startTimings(LifecycleState.IN_BETWEEN);
        } catch (Exception ex) {
            if (this.packetHandler.getProtocolState(PacketBound.SERVER) == PacketState.HANDSHAKE) {
                // failed to handle handshake packet
                // most likely something pinging this server without following the Minecraft protocol

                UnsafeChannelCloser.closeForcibly(ctx.channel());
                return;
            }

            if (packet != null && HoneyPlugin.get().getLogger().isDebug()) {
                Bukkit.getLogger().log(Level.WARNING,
                        "Error occurred while handling packet " + packet.getType() + " (id " + packet.getType().getMinecraftID().get()
                                + ") sent by " + this.player.name + ", " + buf.readableBytes() + " bytes", ex);
            }

            if (serializer.refCnt() > 0) {
                serializer.release();
            }

            this.player.failPacket(ex, packet);
        }
    }
}
