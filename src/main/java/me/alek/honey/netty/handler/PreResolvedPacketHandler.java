package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.ChannelCloseInspection;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;

import java.util.function.Function;

public class PreResolvedPacketHandler extends ChannelDuplexHandler {

    public static final String NAME = "pre-resolved-packet-handler";

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
                PacketContainer<WP> packet = (PacketContainer<WP>) InboundPacketFilter.MARK.get();
                if (packet != null && packet.isHandled()) {
                    InboundPacketFilter.removeMarker();
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

                InboundPacketFilter.removeMarker();
                return newPacket;
            case CLIENT:
                return HoneyPlugin.get().getPacketLibrary().getPacketProcessor().writeObjectPacket(this.packetHandler.getPlayer(), msg);
        }

        throw new IllegalStateException();
    }

    private <WP extends SourcedWrappedPacket<WP>> void streamToPipeline(Object msg, PacketContainer<WP> packetContainer, Function<PacketContainer<WP>, Boolean> handling, Runnable sending) {
        if (msg instanceof ByteBuf) {
            sending.run();
            return;
        }

        if (!handling.apply(packetContainer)) {
            return;
        }

        sending.run();

        Callback postAction = packetContainer.getPostAction();
        if (postAction != null) {
            postAction.call(packetContainer);
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

        PacketContainer<?> packet = initPacketContainer(msg, PacketBound.SERVER);
        if (packet == null) {
            return;
        }
        if (packet.isHandled()) {
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            return;
        }

        streamToPipeline(msg, packet, this.packetHandler::read, () -> ctx.fireChannelRead(ReferenceCountUtil.retain(msg)));
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

        PacketContainer<?> packet = initPacketContainer(msg, PacketBound.CLIENT);
        if (packet == null) {
            return;
        }
        if (packet.isHandled()) {
            ctx.write(ReferenceCountUtil.retain(msg));
            return;
        }
        packet.handle();

        this.packetHandler.getProtocolSwapHandler().swapProtocolOutbound(packet);
        streamToPipeline(msg, packet, this.packetHandler::write, () -> ctx.write(ReferenceCountUtil.retain(msg), promise));
        this.packetHandler.getProtocolSwapHandler().swapProtocolOutboundPostPipeline(packet);
    }

    public void checkRelocation(PacketBound bound) {
        try {
            this.resolving = true;
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocatePreparationHandlers(this.packetHandler.getChannel());
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
