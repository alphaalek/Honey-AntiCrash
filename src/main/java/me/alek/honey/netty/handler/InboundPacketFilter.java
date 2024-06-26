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
import me.alek.honey.util.LazyInit;
import me.alek.honey.validation.impl.FloodValidations;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.CancellableByteBuf;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;

@ChannelHandler.Sharable
public class InboundPacketFilter extends MessageToMessageDecoder<ByteBuf> {

    public static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> MARK = new ThreadLocal<>();

    private boolean hasCalledChannelInactive;
    private final HoneyPlayer player;
    private final HoneyPacketHandler packetHandler;

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
        HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ejectChannel(ctx.channel());
        hasCalledChannelInactive = true;
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {
        if (!byteBuf.isReadable()) {
            ctx.fireChannelRead(byteBuf);
            return;
        }

        int id = new PacketDataSerializer(byteBuf).readVarInt();
        byteBuf.resetReaderIndex();
        PacketType type = packetHandler.getPacketType(PacketBound.SERVER, id);

        Bukkit.getLogger().info("TYPE " + type + " " + id);

        PacketContainer<? extends WrappedPacket<?>> packetContainer = new InternalPacketContainer<>(player, ctx.channel(), type);

        if (MARK.get() != null
                && HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(ctx.channel()) != null
                && MARK.get().getType() != null) {
            Bukkit.getLogger().info("Early drop " + ctx.pipeline().toMap());
            FloodValidations.TOO_EARLY_DROP_PIPELINE.doStream(
                    player,
                    (validation, container) -> validation.validate(player, 1, container),
                    packetContainer);
        }

        MARK.set(packetContainer);
        packetContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_HANDLE);

        CancellableByteBuf cancellableByteBuf = new CancellableByteBuf(player, byteBuf);
        HoneyPlugin.get().getPacketLibrary().getPacketProcessor().callDecode(player, type, cancellableByteBuf);
        packetContainer.getEventloopStateTimings().stopTimings();

        if (!cancellableByteBuf.isCancelled()) {
            list.add(byteBuf.readBytes(byteBuf.readableBytes()));
        } else {
            list.add(Unpooled.buffer());
        }

        packetContainer.getEventloopStateTimings().startTimings(EventloopState.IN_BETWEEN);
    }
}
