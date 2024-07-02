package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.via.ViaProtocol;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.FieldAccessor;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;
import me.alek.honey.validation.impl.FloodValidations;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.CancellableByteBuf;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.stream.Collectors;

@ChannelHandler.Sharable
public class InboundPacketFilter extends MessageToMessageDecoder<ByteBuf> {

    public static final ThreadLocal<PacketContainer<? extends WrappedPacket<?>>> MARK = new ThreadLocal<>();
    public static final Object PAPER_COMPRESSION_ENABLED_EVENT = getPaperCompressionEnabledEvent();

    private static Object getPaperCompressionEnabledEvent() {
        try {
            Class<?> eventClass = Reflection.getClass("io.papermc.paper.network.ConnectionEvent");
            Bukkit.broadcastMessage(Arrays.stream(Reflection.getDeclaredFields(eventClass)).map(FieldAccessor::new).map(FieldAccessor::getName).collect(Collectors.toList()) + "");
            return Reflection.getField(eventClass, "COMPRESSION_THRESHOLD_SET").get(null);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private final HoneyPlayer player;
    private final HoneyPacketHandler packetHandler;
    private boolean hasCalledChannelInactive;
    private ViaProtocol viaProtocol;

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
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        super.userEventTriggered(ctx, event);

        Bukkit.broadcastMessage("RECEIVED EVENT " + event);

        if (event.equals(PAPER_COMPRESSION_ENABLED_EVENT)) {
            // let ViaVersion inject their handlers first
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(ctx.channel());
        }
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) {
        if (!buf.isReadable() || packetHandler.isResolving(PacketBound.SERVER)) {
            ctx.fireChannelRead(buf);
            return;
        }

        packetHandler.handleEventloopProcesses(PacketBound.SERVER);

        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        int id = Types.VAR_INT.read(serializer);
        buf.resetReaderIndex();

        PacketState state = packetHandler.getProtocolState(PacketBound.SERVER);
        PacketType type = null;

        // check if the client major version is not the same as the server
        // if it is not the same, then we will acquire the packet type with ViaVersion
        if (ViaProtocol.ViaValidator.VALIDATOR.find().get()
                && state != PacketState.HANDSHAKE
                && !player.clientInfo.getMajorProtocol().equals(NetProtocol.getProtocol())) {

            if (viaProtocol == null) {
                viaProtocol = ViaProtocol.getViaProtocolFrom(player.clientInfo.getMajorProtocol());
            }

            if (viaProtocol.hasState(PacketBound.SERVER, state)) {
                type = viaProtocol.getServerboundMappings(state).get(id);
            }
        } else {
            type = packetHandler.getPacketType(PacketBound.SERVER, id);
        }

        PacketContainer<? extends WrappedPacket<?>> packetContainer = new InternalPacketContainer<>(player, ctx.channel(), type, buf.readableBytes());

        if (Optional.ofNullable(MARK.get()).map(PacketContainer::getType).isPresent()
                && HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(ctx.channel()) != null) {

            FloodValidations.TOO_EARLY_DROP_PIPELINE.doStream(
                    player,
                    (validation, context) -> validation.validate(player, 1, context),
                    packetContainer);
        }

        MARK.set(packetContainer);
        packetContainer.getEventloopStateTimings().startTimings(EventloopState.HONEY_HANDLE);

        CancellableByteBuf cancellableByteBuf = new CancellableByteBuf(player, buf);
        HoneyPlugin.get().getPacketLibrary().getPacketProcessor().callDecode(player, type, cancellableByteBuf);
        packetContainer.getEventloopStateTimings().stopTimings();

        if (!cancellableByteBuf.isCancelled()) {
            list.add(buf.readBytes(buf.readableBytes()));
        } else {
            list.add(Unpooled.buffer());
        }

        packetContainer.getEventloopStateTimings().startTimings(EventloopState.IN_BETWEEN);
    }
}
