package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Callback;

import java.util.function.Function;

public class PreResolvedPacketHandler extends ChannelDuplexHandler {

    private final PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();
    private final HoneyPacketHandler packetHandler;

    public boolean read = true;
    public boolean write = true;
    private boolean hasBeenRelocated;
    private boolean resolving;

    public PreResolvedPacketHandler(HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    public HoneyPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public boolean streams(PacketBound bound) {
        return bound.equals(PacketBound.SERVER) ? read : write;
    }

    @SuppressWarnings("unchecked")
    private <WP extends WrappedPacket<WP>> PacketContainer<WP> initPacketContainer(Channel channel, Object msg, PacketBound bound) {
        switch (bound) {
            case SERVER:
                PacketContainer<WP> packetContainer = (PacketContainer<WP>) InboundPacketFilter.MARK.get();
                InboundPacketFilter.MARK.remove();
                return packetProcessor.read(packetHandler.getPlayer(), msg, packetContainer);
            case CLIENT:
                return packetProcessor.write(packetHandler.getPlayer(), msg);
        }

        throw new IllegalStateException();
    }

    private <WP extends WrappedPacket<WP>> void sendThroughPipeline(Object msg, PacketContainer<WP> packetContainer, Function<PacketContainer<WP>, Boolean> handling, Runnable sending) {
        if (msg instanceof ByteBuf) {
            sending.run();
            return;
        }

        if (!handling.apply(packetContainer)) {
            return;
        }

        sending.run();

        Callback postAction = packetContainer.getPost();
        if (postAction != null) {
            postAction.call(packetContainer);
        }
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (resolving) {
            ctx.fireChannelRead(msg);
            return;
        }
        checkRelocation();

        if (!packetHandler.isPacket(msg)) {
            if (!packetHandler.handleNonPacket(msg)) {
                return;
            }
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            return;
        }

        if (!read) {
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            return;
        }

        try {
            PacketContainer<? extends WrappedPacket<?>> packetContainer = initPacketContainer(ctx.channel(), msg, PacketBound.SERVER);
            if (packetContainer == null) {
                return;
            }
            sendThroughPipeline(msg, packetContainer, packetHandler::read, () -> ctx.fireChannelRead(msg));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (resolving) {
            ctx.write(msg, promise);
            return;
        }

        packetHandler.runDelayedTasks(PacketBound.CLIENT);
        checkRelocation();

        if (!packetHandler.isPacket(msg)) {
            if (!packetHandler.handleNonPacket(msg)) {
                promise.setSuccess();
                return;
            }
            ctx.write(msg, promise);
            return;
        }

        if (!write) {
            ctx.write(ReferenceCountUtil.retain(msg));
            return;
        }

        PacketContainer<? extends WrappedPacket<?>> packetContainer = initPacketContainer(ctx.channel(), msg, PacketBound.CLIENT);
        if (packetContainer == null) {
            return;
        }
        sendThroughPipeline(msg, packetContainer, packetHandler::write, () -> ctx.write(msg, promise));
    }

    public void checkRelocation() {
        if (!hasBeenRelocated && !resolving) {
            try {
                resolving = true;
                hasBeenRelocated = HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocatePreparationHandlers(packetHandler.getChannel());
            } catch (MissingHandlerException ex) {
                hasBeenRelocated = false;
            }
            resolving = false;
        }
    }
}
