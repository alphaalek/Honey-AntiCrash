package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.HoneyPacketCodec;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.WrappedPacketFactoryCollection;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@ChannelHandler.Sharable
public class HoneyPacketInStream extends MessageToMessageDecoder<ByteBuf> {

    private final HoneyPacketHandler packetHandler;
    private final HoneyPlayer player;
    private MethodHandle decodeInvoker;
    private ChannelHandler decoder;

    public HoneyPacketInStream(Channel channel, HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        this.player = packetHandler.getPlayer();
        setupMethodInvoke(channel.pipeline());
    }

    public HoneyPacketHandler getPacketHandler() {
        return this.packetHandler;
    }

    private void setupMethodInvoke(ChannelPipeline pipeline) {
        try {
            this.decoder = pipeline.get(PacketInterceptorResolver.getDecoderPosition(pipeline));
            Method method = this.decoder.getClass().getDeclaredMethod("decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
            method.setAccessible(true);
            this.decodeInvoker = MethodHandles.lookup().unreflect(method);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to setup packet decoder environment!", ex);
        }
    }

    private void stopTimings(PacketContainer<?> packet) {
        if (packet.getEventloopStateTimings().getCurrentState() == LifecycleState.NO_STATE) {
            return;
        }

        packet.getEventloopStateTimings().stopTimings();

        if (this.player.linkedBukkit()) {
            HoneyPlugin.get().getNettyPacketTracker().registerTime(player,
                    packet.getEventloopStateTimings().getTimeFor(LifecycleState.HONEY_HANDLE)
                            + packet.getEventloopStateTimings().getTimeFor(LifecycleState.IN_BETWEEN)
                            + packet.getEventloopStateTimings().getTimeFor(LifecycleState.DECODE)
            );
        }
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) {
        if (!this.player.isStreamActive()) {
            return;
        }

        if (!buf.isReadable()) {
            list.add(buf.retain());
            return;
        }

        internalRead(ctx, buf, list);
        if (this.packetHandler.filterMark != null) {
            this.packetHandler.filterMark.releaseIfAutoReleasable();
            this.packetHandler.filterMark = null;
            InboundPacketFilter.MARK.remove();
        }
    }

    private void internalRead(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) {
        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        PacketContainer<?> packet = null;
        PacketType type = null;
        try {
            packet = this.packetHandler.filterMark;
            type = this.packetHandler.getPacketType(serializer, false, PacketBound.SERVER, false);
            Bukkit.getLogger().info("Read " + type);
            if (PacketTypes.isUnknown(type)) {
                // invalid packet
                throw new DecoderException("Invalid packet, state " + this.packetHandler.getProtocolState(PacketBound.SERVER) + ", id " + serializer.readerIndex(0).readVarInt());
            }

            if (packet == null || !packet.getType().equals(type)) {
                // this packet is not sent by the client (because it was not received by the filter),
                // but instead invoked by some plugin (most likely ViaVersion)
                if (packet == null) {
                    packet = new InternalPacketContainer<>(PacketContainer.PipelineState.FINAL_BYTE, serializer, this.player, type, null);
                } else {
                    packet.handleUnknownRewritten(type, PacketContainer.PipelineState.FINAL_BYTE);
                }

                // calls listeners that was not called, due to some plugin invoking this packet in the eventloop after the filter
                packet.callLateInitialByteListeners(serializer);
            }
            // handle final byte state for packets that has not been rewritten
            if (packet.getCreatedPipelineState().equals(PacketContainer.PipelineState.INITIAL_BYTE)
                    && WrappedPacketFactoryCollection.hasByteWrappedPacket(packet.getType(), PacketContainer.PipelineState.FINAL_BYTE)) {
                packet.callPreMinecraftDecode(serializer);
            }

            // stops in-between timings
            packet.getEventloopStateTimings().stopTimings();

            // rewrite this packet if any implementation is present
            if (packet.getFinalByteWrapped() != null) {
                serializer = packet.rewrite(serializer);
            } else {
                serializer.retain();
            }

            // set reader index back to 0 for the invoking process
            invokeRead(packet, ctx, serializer.readerIndex(0), list);
        } catch (Exception ex) {
            if (HoneyPlugin.get().getLogger().isDebug()) {
                type = Optional.ofNullable(type).orElse(PacketType.UNKNOWN);
                HoneyPlugin.get().getLogger().getLogger().log(Level.WARNING,
                        "Error occurred while handling packet " + type + " (id " + type.getID()
                                + ") sent by " + this.player.name + ", " + (serializer.readerIndex() + serializer.readableBytes()) + " bytes", ex);
            }
            this.player.failPacket(ex, packet);
        }
    }

    private void invokeRead(PacketContainer<?> packet, ChannelHandlerContext ctx, PacketDataSerializer serializer, List<Object> list) {
        packet.getEventloopStateTimings().startTimings(LifecycleState.DECODE);
        try {
            Object decodedImpl;
            try {
                if (!ConfigurationOptions.Decoder.USE_DEFAULT) {
                    decodedImpl = HoneyPacketCodec.decodePacket(this.player, serializer);
                } else {
                    // explicit set variable, so Proguard recognizes the method
                    Object empty = this.decodeInvoker.invokeWithArguments(new Object[]{this.decoder, ctx, serializer, list});
                    if (list.isEmpty()) {
                        stopTimings(packet);
                        return;
                    }
                    decodedImpl = list.get(0);
                    list.clear();
                }
            } catch (Throwable ex) {
                if (HoneyPlugin.get().getLogger().isDebug()) {
                    HoneyPlugin.get().getLogger().getLogger().log(Level.WARNING,
                            "Error occurred while decoding packet " + packet.getType() + " (id " + packet.getType().getID()
                                    + ") sent by " + this.player.name + ", " + (serializer.readerIndex() + serializer.readableBytes()) + " bytes", ex);
                }
                list.clear();
                this.player.failPacket(ex, packet);
                return;
            }
            if (decodedImpl == null) {
                stopTimings(packet);
                return;
            }
            packet.getEventloopStateTimings().startTimings(LifecycleState.HONEY_HANDLE);

            // link object instance with expected packet type
            Optional<PacketType> typeOptional = PacketTypes.getPacketType(decodedImpl.getClass(), this.packetHandler.getProtocolState(PacketBound.SERVER));
            if (!typeOptional.isPresent()) {
                HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "Failed to link object-instance to expected packet type, for " + packet.getType() + " sent by " + this.player.name + "; " + decodedImpl.getClass());
                return;
            }
            packet.callPostMinecraftDecode(decodedImpl, typeOptional.get());

            Runnable packetExecutorTask;
            if (!packet.isCancelled()) {
                packetExecutorTask = HoneyPlugin.get().getPacketLibrary().getPacketProcessor().getPacketExecutorTask(packet);
            } else {
                packetExecutorTask = null;
            }

            // stop all timings associated with this packet
            stopTimings(packet);

            if (!this.packetHandler.read(packet)) {
                return;
            }
            packet.handle();
            this.packetHandler.getProtocolSwapHandler().swapProtocolInbound(packet);

            // check if this packet should be submitted to the main thread directly, and ignore the rest of the pipeline
            // and the player connection handler's implementation of handling this packet
            if (packetExecutorTask != null) {
                PreMainThreadInterceptor.POST_TO_MAIN_THREAD.set(packet);
                HoneyPlugin.get().getExecutionQueueProxy().getExecutionQueue().add(packetExecutorTask);
                // drop the packet for the pipeline
                return;
            }

            // fire the decoded packet to the rest of the pipeline and finally submit it to the player connection handler
            list.add(decodedImpl);

            // handle registered post actions
            Callback postAction = packet.getPostAction();
            if (postAction != null) {
                postAction.call(packet);
            }
        } catch (Throwable ex) {
            try {
                super.exceptionCaught(ctx, ex);
            } catch (Exception ignored) {
                // to do nothing
            }

            stopTimings(packet);
        }
    }
}