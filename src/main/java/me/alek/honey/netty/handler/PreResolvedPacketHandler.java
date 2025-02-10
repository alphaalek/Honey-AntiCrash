package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;

public class PreResolvedPacketHandler extends ChannelDuplexHandler {

    public static final String NAME = "honey-pre-resolved-packet-handler";

    private final HoneyPacketHandler packetHandler;

    public boolean read = true;
    public boolean write = true;
    private boolean resolving;

    public PreResolvedPacketHandler(HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    public HoneyPacketHandler getPacketHandler() {
        return this.packetHandler;
    }

    @SuppressWarnings("unchecked")
    private <WP extends SourcedWrappedPacket<WP>> PacketContainer<WP> initPacketContainer(Object msg, PacketBound bound) {
        switch (bound) {
            case SERVER:
                PacketContainer<WP> packet = (PacketContainer<WP>) this.packetHandler.filterMark;
                this.packetHandler.filterMark = null;
                InboundPacketFilter.MARK.remove();
                if (packet != null && packet.isHandled()) {
                    return packet;
                }

                PacketContainer<WP> newPacket = HoneyPlugin.get().getPacketLibrary().getPacketProcessor().readObjectPacket(this.packetHandler.getPlayer(), msg, packet);
                if (newPacket != null && (packet == null || newPacket.getType().equals(packet.getType()))) {
                    this.packetHandler.getProtocolSwapHandler().swapProtocolInbound(newPacket);
                } else {
                    if (packet != null) {
                        this.packetHandler.getProtocolSwapHandler().swapProtocolInbound(packet);
                    }
                    if (newPacket != null) {
                        this.packetHandler.getProtocolSwapHandler().swapProtocolInbound(newPacket);
                    }
                }
                return newPacket;
            case CLIENT:
                return HoneyPlugin.get().getPacketLibrary().getPacketProcessor().writeObjectPacket(this.packetHandler.getPlayer(), msg);
            default:
                throw new IllegalArgumentException("Illegal bound");
        }
    }

    private <WP extends SourcedWrappedPacket<WP>> void streamToPipeline(Object msg, PacketContainer<WP> packet, Function<PacketContainer<WP>, Boolean> handling, Runnable sending) {
        if (msg instanceof ByteBuf) {
            sending.run();
            return;
        }

        if (!handling.apply(packet)) {
            return;
        }

        sending.run();

        Callback postAction = packet.getPostAction();
        if (postAction != null) {
            postAction.call(packet);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (this.resolving || !this.read) {
            ctx.fireChannelRead(msg);
            return;
        }

        checkRelocation(PacketBound.SERVER);
        if (!this.packetHandler.isPacket(msg)) {
            ctx.fireChannelRead(msg);
            return;
        }

        PacketContainer<?> packet = null;
        try {
            packet = initPacketContainer(msg, PacketBound.SERVER);
            if (packet == null) {
                return;
            }
            if (packet.isHandled()) {
                ctx.fireChannelRead(msg);
                return;
            }

            streamToPipeline(msg, packet, this.packetHandler::read, () -> ctx.fireChannelRead(ReferenceCountUtil.retain(msg)));
        } catch (Exception ex) {
            if (HoneyPlugin.get().getLogger().isDebug()) {
                PacketType type = PacketTypes.getPacketType(msg.getClass(), this.packetHandler.getProtocolState(PacketBound.SERVER)).orElse(PacketType.UNKNOWN);
                HoneyPlugin.get().getLogger().getLogger().log(Level.WARNING,
                        "Error occurred while handling packet " + type + " (id " + type.getID()
                                + ") sent by " + this.packetHandler.getPlayer().name + ", " + Optional.ofNullable(packet).map(p -> p.getAnyByteWrapped().getByteSize()).orElse(-1) + " bytes", ex);
            }

            this.packetHandler.getPlayer().failPacket(ex, packet);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (this.resolving || !this.write) {
            ctx.write(msg, promise);
            return;
        }

        checkRelocation(PacketBound.CLIENT);
        // don't handle this packet right now if the out stream has been relocated into the pipeline,
        // because then that handler will handle it instead
        if (!this.write || !this.packetHandler.isPacket(msg)) {
            ctx.write(msg, promise);
            return;
        }

        PacketContainer<?> packet = null;
        try {
            packet = initPacketContainer(msg, PacketBound.CLIENT);
            if (packet == null) {
                promise.setSuccess();
                return;
            }
            if (packet.isHandled()) {
                ctx.write(msg, promise);
                return;
            }
            packet.handle();

            this.packetHandler.getProtocolSwapHandler().swapProtocolOutbound(packet);
            streamToPipeline(msg, packet, this.packetHandler::write, () -> ctx.write(msg, promise));
            this.packetHandler.getProtocolSwapHandler().swapProtocolOutboundPostPipeline(packet);
        } catch (Exception ex) {
            if (HoneyPlugin.get().getLogger().isDebug()) {
                PacketType type = Optional.ofNullable(packet)
                        .map(PacketContainer::getType)
                        .orElse(PacketTypes.getPacketType(msg.getClass(), this.packetHandler.getProtocolState(PacketBound.CLIENT)).orElse(PacketType.UNKNOWN));
                HoneyPlugin.get().getLogger().getLogger().log(Level.WARNING, "Error occurred while handling packet " + type + " (id " + type.getID() + ") sent by " + this.packetHandler.getPlayer().name, ex);
            }
            promise.setSuccess();
        }
    }

    public void checkRelocation(PacketBound bound) {
        try {
            this.resolving = true;
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocatePreparationHandlers(this.packetHandler.getChannel(), bound);
            if (bound.equals(PacketBound.SERVER) && HoneyPlugin.get().getPacketLibrary().getChannelManager().getInStream(this.packetHandler.getChannel()) != null) {
                this.read = false;
            } else if (bound.equals(PacketBound.CLIENT) && HoneyPlugin.get().getPacketLibrary().getChannelManager().getOutStream(this.packetHandler.getChannel()) != null) {
                this.write = false;
            }
        } catch (MissingHandlerException ex) {
            ChannelCloseInspection.close(this.getPacketHandler().getChannel());
        }
        this.resolving = false;
    }
}
