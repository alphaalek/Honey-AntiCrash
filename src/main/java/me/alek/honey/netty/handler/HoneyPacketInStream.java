package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.packet.InternalPacketContainer;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@ChannelHandler.Sharable
public class HoneyPacketInStream extends MessageToMessageDecoder<ByteBuf> {

    private final HoneyPacketHandler packetHandler;
    private final HoneyPlayer player;

    public HoneyPacketInStream(HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        this.player = packetHandler.getPlayer();
    }

    public HoneyPacketHandler getPacketHandler() {
        return this.packetHandler;
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

        internalRead(buf, list);
        if (this.packetHandler.filterMark != null) {
            this.packetHandler.filterMark.releaseIfAutoReleasable();
            this.packetHandler.filterMark = null;
            InboundPacketFilter.MARK.remove();
        }
    }

    private void internalRead(ByteBuf buf, List<Object> list) {
        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        PacketContainer<?> packet = null;
        PacketType type = null;
        try {
            packet = this.packetHandler.filterMark;
            int startRidx = serializer.readerIndex();
            type = this.packetHandler.getPacketType(serializer, false, PacketBound.SERVER, false);
            if (PacketTypes.isUnknown(type)) {
                // invalid packet
                throw new DecoderException("Invalid packet, state " + this.packetHandler.getProtocolState(PacketBound.SERVER) + ", id " + serializer.readerIndex(0).readVarInt());
            }

            if (packet == null || !packet.getType().equals(type)) {
                int ridx = serializer.readerIndex();
                // this packet is not sent by the client (because it was not received by the filter),
                // but instead invoked by some plugin (most likely ViaVersion)
                if (packet == null) {
                    packet = new InternalPacketContainer<>(PacketContainer.PipelineState.FINAL_BYTE, serializer, this.player, type, null);
                    if (this.packetHandler.isCancelled(packet)) {
                        // cancelled during initializing
                        return;
                    }
                } else {
                    packet.handleRewrittenByPipeline(type);
                    packet.callPreMinecraftDecode(serializer);
                }

                // calls listeners that was not called, due to some plugin invoking this packet in the eventloop after the filter
                Rewritable.Result<PacketDataSerializer> rewritable = packet.callLateInitialByteListeners(serializer.readerIndex(ridx), startRidx);
                if (rewritable.isRewritten()) {
                    // rewrite
                    serializer = rewritable.get();
                }
            } else {
                packet.callPreMinecraftDecode(serializer);
            }

            // stops in-between timings
            packet.getEventloopStateTimings().stopTimings();

            serializer.readerIndex(startRidx);
            // rewrite this packet if any implementation is present and if it hasn't already been rewritten
            if (packet.getLastRewrittenPipelineState() == null && packet.getFinalByteWrapped() != null) {
                serializer = packet.rewrite(serializer);
            } else {
                serializer.retain();
            }

            this.packetHandler.getProtocolSwapHandler().swapProtocolInbound(packet);
            if (!this.packetHandler.readPacketAtFinalByte(packet)) {
                return;
            }

            // starts decode timings
            packet.getEventloopStateTimings().startTimings(LifecycleState.DECODE);

            // we are finally ready for this packet to be decoded, so invoke the serializer to the pipeline
            list.add(serializer);
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
}