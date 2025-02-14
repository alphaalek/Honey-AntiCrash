package me.alek.honey.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;

import java.util.Optional;
import java.util.logging.Level;

@ChannelHandler.Sharable
public class PreMainThreadInterceptor extends ChannelInboundHandlerAdapter {

    public static final String NAME = "honey-pre-main-thread-interceptor";

    public static final ThreadLocal<PacketContainer<?>> POST_TO_MAIN_THREAD = new ThreadLocal<>();
    private final HoneyPacketHandler packetHandler;

    public PreMainThreadInterceptor(HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!this.packetHandler.isPacket(msg)) {
            ctx.fireChannelRead(msg);
            return;
        }

        PacketContainer<?> packet = this.packetHandler.finalMark;
        this.packetHandler.finalMark = null;
        if (packet != null) {
            handlePacket(packet, ctx, msg);
        } else {
            HoneyPlugin.get().getLogger().getLogger().severe("Intercepted packet that didn't go through the pipeline properly? Please fix...");
        }
    }

    public void handlePacket(PacketContainer<?> packet, ChannelHandlerContext ctx, Object decodedImpl) {
        packet.getEventloopStateTimings().startTimings(LifecycleState.HONEY_HANDLE);

        // link object instance with expected packet type
        Optional<PacketType> typeOptional = PacketTypes.getPacketType(decodedImpl.getClass(), packet.getState());
        if (!typeOptional.isPresent() || !typeOptional.get().equals(packet.getType())) {
            HoneyPlugin.get().getLogger().getLogger().severe("Failed to link object-instance to expected packet type, for " + packet.getType() + " sent by " + this.packetHandler.getPlayer().name + "; " + decodedImpl.getClass() + ", " + typeOptional.orElse(PacketType.UNKNOWN));
            return;
        }
        packet.callPostMinecraftDecode(decodedImpl, typeOptional.get());

        // stop honey-handle timings and register timings for all states
        packet.getEventloopStateTimings().stopTimings();
        if (this.packetHandler.getPlayer().linkedBukkit()) {
            HoneyPlugin.get().getNettyPacketTracker().registerTime(this.packetHandler.getPlayer(),
                    packet.getEventloopStateTimings().getTimeFor(LifecycleState.HONEY_HANDLE)
                            + packet.getEventloopStateTimings().getTimeFor(LifecycleState.IN_BETWEEN)
                            + packet.getEventloopStateTimings().getTimeFor(LifecycleState.DECODE)
            );
        }

        POST_TO_MAIN_THREAD.set(packet);
        Runnable packetExecutorTask;
        if (!packet.isCancelled()) {
            packetExecutorTask = HoneyPlugin.get().getPacketLibrary().getPacketProcessor().getPacketExecutorTask(packet);
        } else {
            packetExecutorTask = null;
        }
        // check if this packet should be submitted to the main thread directly, and ignore the rest of the pipeline
        // and the player connection handler's implementation of handling this packet
        if (packetExecutorTask != null) {
            HoneyPlugin.get().getExecutionQueueProxy().getExecutionQueue().add(packetExecutorTask);
        } else {
            // if not, submit the packet to the main thread as normal behaviour
            ctx.fireChannelRead(decodedImpl);
        }
    }
}
