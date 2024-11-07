package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;

import java.util.function.Function;

public class PreResolvedPacketHandler extends ChannelDuplexHandler {

    private final HoneyPacketHandler packetHandler;

    public boolean read = true;
    public boolean write = true;
    private boolean hasBeenRelocated;
    private boolean resolving;

    public PreResolvedPacketHandler(HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    public HoneyPacketHandler getPacketHandler() {
        return this.packetHandler;
    }

    public boolean streams(PacketBound bound) {
        return bound.equals(PacketBound.SERVER) ? this.read : this.write;
    }

    @SuppressWarnings("unchecked")
    private <WP extends WrappedPacket<WP>> PacketContainer<WP> initPacketContainer(Object msg, PacketBound bound) {
        switch (bound) {
            case SERVER:
                PacketContainer<WP> packet = (PacketContainer<WP>) InboundPacketFilter.MARK.get();
                InboundPacketFilter.MARK.remove();
                return HoneyPlugin.get().getPacketLibrary().getPacketProcessor().read(this.packetHandler.getPlayer(), msg, packet);
            case CLIENT:
                return HoneyPlugin.get().getPacketLibrary().getPacketProcessor().write(this.packetHandler.getPlayer(), msg);
        }

        throw new IllegalStateException();
    }

    private <WP extends WrappedPacket<WP>> void streamToPipeline(Object msg, PacketContainer<WP> packetContainer, Function<PacketContainer<WP>, Boolean> handling, Runnable sending) {
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
        if (this.resolving) {
            ctx.fireChannelRead(msg);
            return;
        }
        checkRelocation();

        if (!this.packetHandler.isPacket(msg)) {
            if (!this.packetHandler.handleNonPacket(msg)) {
                return;
            }
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            return;
        }

        if (!this.read) {
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            return;
        }

        try {
            PacketContainer<? extends WrappedPacket<?>> packet = initPacketContainer(msg, PacketBound.SERVER);
            if (packet == null) {
                return;
            }
            streamToPipeline(msg, packet, this.packetHandler::read, () -> ctx.fireChannelRead(ReferenceCountUtil.retain(msg)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (this.resolving) {
            ctx.write(msg, promise);
            return;
        }

        this.packetHandler.runDelayedTasks(PacketBound.CLIENT);
        checkRelocation();

        if (!this.packetHandler.isPacket(msg)) {
            if (!this.packetHandler.handleNonPacket(msg)) {
                promise.setSuccess();
                return;
            }
            ctx.write(msg, promise);
            return;
        }

        if (!this.write) {
            ctx.write(ReferenceCountUtil.retain(msg));
            return;
        }

        PacketContainer<? extends WrappedPacket<?>> packet = initPacketContainer(msg, PacketBound.CLIENT);
        if (packet == null) {
            return;
        }
        streamToPipeline(msg, packet, this.packetHandler::write, () -> ctx.write(ReferenceCountUtil.retain(msg), promise));
    }

    public void checkRelocation() {
        if (!this.hasBeenRelocated && !this.resolving) {
            try {
                this.resolving = true;
                this.hasBeenRelocated = HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocatePreparationHandlers(packetHandler.getChannel());
            } catch (MissingHandlerException ex) {
                this.hasBeenRelocated = false;
            }
            this.resolving = false;
        }
    }
}
