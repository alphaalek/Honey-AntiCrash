package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.configuration.PacketLimitHandlerType;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.codec.HoneyPacketCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.StackDecodingException;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.reflection.Reflection;
import me.alek.honey.validation.HoneyValidation;
import me.alek.honey.validation.ValidationPipeline;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.ViolationManagement;
import me.alek.honey.validation.impl.FloodValidations;
import org.bukkit.Bukkit;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.logging.Level;
import java.util.stream.Collectors;

@ChannelHandler.Sharable
public class InboundPacketFilter extends MessageToMessageDecoder<ByteBuf> {

    private static final String PIPELINE_FIX_HANDLER = "honey-pipeline-fix";
    private static final Object PIPELINE_FIX_MARK = new Object();

    private static final String CONNECTION_EVENT_CLASS_NAME = "io.papermc.paper.network.ConnectionEvent";
    private static final String COMPRESSION_THRESHOLD_SET_FIELD_NAME = "COMPRESSION_THRESHOLD_SET";
    private static final Object PAPER_COMPRESSION_ENABLED_EVENT = getPaperCompressionEnabledEvent();

    public static final ThreadLocal<PacketContainer<?>> MARK = new ThreadLocal<>();

    private static final HoneyValidation<Integer> STACK_PROCESS_VALIDATION = HoneyValidation.of("Item Processing", "Could not process item stack", IDRegistry.VALIDATION.acquire());
    private static final ConfigurationOptions.ViolationSetting STACK_PROCESS_SETTING = new ConfigurationOptions.ViolationSetting(PacketLimitHandlerType.SIMPLE);

    private static Object getPaperCompressionEnabledEvent() {
        try {
            Class<?> eventClass = Reflection.getClass(CONNECTION_EVENT_CLASS_NAME);
            return Reflection.getField(eventClass, COMPRESSION_THRESHOLD_SET_FIELD_NAME).get(null);
        } catch (Exception ex) {
            return null;
        }
    }

    public static void removeMarker() {
        MARK.remove();
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
        if (ctx.pipeline().get(PIPELINE_FIX_HANDLER) == null) {
            ctx.pipeline().addFirst(PIPELINE_FIX_HANDLER, new ChannelDuplexHandler() {

                private final Queue<Object> queuedDataFromForcedStream = new LinkedList<>();

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    this.queuedDataFromForcedStream.add(msg);
                }

                @Override
                public void write(ChannelHandlerContext fixCtx, Object msg, ChannelPromise promise) {
                    if (msg != PIPELINE_FIX_MARK && msg instanceof ByteBuf) {
                        throw new IllegalStateException("Fixing pipeline, can't process packet " + msg);
                    }
                    InboundPacketFilter.this.channelActive(ctx);
                    ctx.pipeline().remove(PIPELINE_FIX_HANDLER);

                    this.queuedDataFromForcedStream.forEach(fixCtx::fireChannelRead);
                    ctx.channel().config().setAutoRead(true);

                    promise.setSuccess();
                }
            });
            ctx.channel().config().setAutoRead(false);
            ctx.fireChannelActive();

            ctx.channel().eventLoop().submit(() -> ctx.write(PIPELINE_FIX_MARK));
        } else {
            if (ctx.channel().remoteAddress() == null) {
                // null ping (1.8.8 bug)
                ChannelCloseInspection.close(ctx.channel());
                return;
            }

            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(ctx.channel());
            this.packetHandler.setProtocolState(PacketBound.SERVER, PacketState.HANDSHAKE);
        }
    }

    @Override
    public synchronized void channelInactive(ChannelHandlerContext ctx) {
        ctx.fireChannelInactive();

        if (!this.hasCalledChannelInactive) {
            this.packetHandler.getAsyncPacketStream().farewell();
        }
        this.hasCalledChannelInactive = true;

        HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ejectChannel(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
        ctx.fireUserEventTriggered(event);

        if (event.equals(PAPER_COMPRESSION_ENABLED_EVENT)) {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(ctx.channel());
        }
    }

    public void handleViaDroppedPacket(PacketContainer<?> packet) {
        this.packetHandler.getProtocolSwapHandler().swapProtocolViaDropped(packet);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) {
        if (!this.player.isStreamActive()) {
            return;
        }

        if (!buf.isReadable() || this.packetHandler.isResolving(PacketBound.SERVER)) {
            ctx.fireChannelRead(buf);
            return;
        }

        this.packetHandler.executeDelayedTasks(PacketBound.SERVER);

        PacketContainer<?> prev = MARK.get();
        // check for too early dropped packet in the previous eventloop
        // if the filter mark has a value, it means that a channel handler between
        // the filter and decode preparation dropped this packet
        // and did not clear the marker properly
        if (prev != null && !PacketTypes.isUnknown(prev.getType()) && !prev.isHandled()) {
            // ignore PONG, TRANSACTION (those are used by AntiCheats), TICK_END and PLAYER_MOVEMENT because the client sends
            // a lot of these packets, and they will all be dropped in the pipeline if the backend server does not support these packets
            if (prev.getType() != PacketTypes.Play.Client.PONG
                    && prev.getType() != PacketTypes.Play.Client.TRANSACTION
                    && prev.getType() != PacketTypes.Play.Client.TICK_END
                    && prev.getType() != PacketTypes.Play.Client.PLAYER_MOVEMENT) {
                HoneyPlayer player = prev.getPlayer();
                FloodValidations.PACKET_DROP_PIPELINE.doStreamOtherPipeline(
                        player,
                        (validation, context) -> validation.validate(player, context),
                        prev);
            }

            InboundPacketFilter.removeMarker();
            prev.releaseIfAutoReleasable();
            this.packetHandler.getProtocolSwapHandler().swapProtocolInbound(prev);
        }

        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        PacketContainer<?> packet = null;
        PacketType type = null;
        try {
            // initialize packet container
            type = this.packetHandler.getPacketType(serializer, false, this.packetHandler.getSimulatedServerboundProtocolState(), PacketBound.SERVER, true);
            if (type == null) {
                // invalid packet
                serializer.resetReaderIndex();
                throw new DecoderException("Invalid packet, state " + this.packetHandler.getSimulatedServerboundProtocolState() + ", id " + serializer.readVarInt());
            }
            packet = new InternalPacketContainer<>(PacketContainer.PipelineState.INITIAL_BYTE, serializer, this.player, type, null);
            MARK.set(packet);

            if (packet.getInitialByteWrapped().isAbstract()) {
                // check if any bytes has not been decoded
                // if any is left, the protocol has not been followed properly
                HoneyPacketCodec.checkReadable(packet.getProtocol(), type, null, serializer);
            }

            // call listeners
            if (!this.player.bypassEverything()) {
                packet.getEventloopStateTimings().startTimings(LifecycleState.HONEY_HANDLE);
                HoneyPlugin.get().getPacketLibrary().getPacketProcessor().callInitialBytePacketProcessListeners(this.player, packet);
                packet.getEventloopStateTimings().stopTimings();
            }

            if (type == PacketTypes.Play.Client.CUSTOM_PAYLOAD) {
                WrappedPlayInCustomPayload.PayloadData<?> data = ((WrappedPlayInCustomPayload<?>) packet.getCurrentWrapped()).getPayloadData();
                if (data.isBookChannel() && data.isItemInitialized()) {
                    HoneyPacketCodec.checkReadable(packet.getProtocol(), packet.getType(), null, data.getSerializer());
                }
            }

            this.packetHandler.getProtocolSwapHandler().swapProtocolInboundFilter(packet);

            if (!this.packetHandler.isCancelled(packet)) {
                list.add(serializer.readerIndex(0).retain());
            } else {
                packet.releaseIfAutoReleasable();
            }

            // start in-between timings - stopped in decode preparation handler
            packet.getEventloopStateTimings().startTimings(LifecycleState.IN_BETWEEN);
        } catch (StackDecodingException ex) {
            ViolationManagement management = HoneyPlugin.get().getViolationManagement();

            ValidationPipelineContext.DevelopmentData data = new ValidationPipelineContext.DevelopmentData();
            ValidationPipeline.applyDevelopmentDataDirectly(data, this.player, STACK_PROCESS_VALIDATION, Optional.ofNullable(type).orElse(PacketType.UNKNOWN), buf.readerIndex() + buf.readableBytes());
            data.addValue(" Error", this.player.getFailedPacketCause(ex));

            management.flag(this.player, STACK_PROCESS_VALIDATION, STACK_PROCESS_SETTING, data);
        } catch (Exception ex) {
            ex.printStackTrace();
            InboundPacketFilter.removeMarker();
            if (this.player.pinging || this.packetHandler.getProtocolState(PacketBound.SERVER) == PacketState.HANDSHAKE) {
                // failed to handle handshake packet
                // most likely something pinging this server without following the Minecraft protocol
                ChannelCloseInspection.close(ctx.channel());
                return;
            }

            if (HoneyPlugin.get().getLogger().isDebug()) {
                PacketType type0 = Optional.ofNullable(type).orElse(PacketType.UNKNOWN);

                int id = Optional.ofNullable(type)
                        .map(PacketType::getMinecraftID)
                        .map(LazyInit::get)
                        .orElse(-1);
                Bukkit.getLogger().log(Level.WARNING,
                        "Error occurred while handling packet " + type0 + " (id " + id
                                + ") sent by " + this.player.name + ", " + (buf.readerIndex() + buf.readableBytes()) + " bytes", ex);
            }

            list.clear();
            this.player.failPacket(ex, packet);
        }
    }
}
