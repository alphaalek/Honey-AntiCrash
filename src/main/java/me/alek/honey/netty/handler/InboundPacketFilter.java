package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.util.reflection.Reflection;
import me.alek.honey.validation.impl.FloodValidations;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

import java.util.*;

@ChannelHandler.Sharable
public class InboundPacketFilter extends MessageToMessageDecoder<ByteBuf> {

    public static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> MARK = new ThreadLocal<>();
    public static final Object PAPER_COMPRESSION_ENABLED_EVENT = getPaperCompressionEnabledEvent();

    private static Object getPaperCompressionEnabledEvent() {
        try {
            Class<?> eventClass = Reflection.getClass("io.papermc.paper.network.ConnectionEvent");
            return Reflection.getField(eventClass, "COMPRESSION_THRESHOLD_SET").get(null);
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
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
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

        if (!buf.isReadable() || packetHandler.isResolving(PacketBound.SERVER)) {
            ctx.fireChannelRead(buf);
            return;
        }

        packetHandler.runDelayedTasks(PacketBound.SERVER);

        PacketContainer<? extends WrappedPacket<?>> prevMarked = MARK.get();
        if (prevMarked != null && !PacketTypes.isUnknown(prevMarked.getType())) {
            FloodValidations.TOO_EARLY_DROP_PIPELINE.doStream(
                    player,
                    (validation, context) -> validation.validate(player, 1, context),
                    prevMarked);
        }

        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.copiedBuffer(buf));
        PacketType type = packetHandler.getPacketType(serializer, false);
        PacketContainer<? extends WrappedPacket<?>> packet = new InternalPacketContainer<>(serializer, player, type, null);

        packet.getEventloopStateTimings().startTimings(LifecycleState.HONEY_HANDLE);

        MARK.set(packet);

        HoneyPlugin.get().getPacketLibrary().getPacketProcessor().callBytePacketProcessListeners(player, packet);
        packet.getEventloopStateTimings().stopTimings();

        if (!packet.isCancelled()) {
            list.add(buf.readBytes(buf.readableBytes()));
        } else {
            list.add(Unpooled.buffer());
        }

        serializer.release();

        packet.getEventloopStateTimings().startTimings(LifecycleState.IN_BETWEEN);
    }
}
