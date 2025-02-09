package me.alek.honey.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.packet.PacketContainer;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

@ChannelHandler.Sharable
public class HoneyDebugger extends ChannelInboundHandlerAdapter {

    private static final String VIA_VERSION_CANCEL = "This packet is supposed to be cancelled; If you have debug enabled, you can ignore these";
    private static final String NETTY_NATIVE_READ_RESET = "Error while read(...): Connection reset by peer";
    private static final String NETTY_NATIVE_WRITE_RESET = "Error while write(...): Connection reset by peer";
    private static final String NETTY_NATIVE_WRITE_BROKEN_PIPE = "Error while write(...): Broken pipe";
    private static final String NETTY_CONNECTION_RESET = "Connection reset";

    private final HoneyPacketHandler packetHandler;

    public HoneyDebugger(HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    public HoneyPacketHandler getPacketHandler() {
        return this.packetHandler;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        onExceptionCaught(ctx.channel(), cause);
        try {
            ctx.fireExceptionCaught(cause);
        } catch (Exception ignored) {
            // to do nothing
        }

        ChannelCloseInspection.close(ctx.channel());
    }

    public void onExceptionCaught(Channel channel, Throwable throwable) {
        String message = Optional.ofNullable(throwable.getMessage()).orElse("");

        if (message.equals(VIA_VERSION_CANCEL)) {
            PacketContainer<?> packet = this.packetHandler.filterMark;
            if (packet != null) {
                HoneyPlugin.get().getPacketLibrary().getChannelManager().getFilter(channel).handleViaDroppedPacket(packet);
            }
            this.packetHandler.filterMark = null;
            return;
        }

        if (HoneyPlugin.get().getLogger().isDebug()
                && /* Java */ !(throwable instanceof ReadTimeoutException)
                && /* Java */ !(throwable instanceof IOException)
                && /* Netty */ !message.equals(NETTY_NATIVE_READ_RESET)
                && /* Netty */ !message.equals(NETTY_NATIVE_WRITE_BROKEN_PIPE)
                && /* Netty */ !message.equals(NETTY_NATIVE_WRITE_RESET)
                && /* Netty */ !message.equals(NETTY_CONNECTION_RESET)) {
            HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "DEBUG: Exception was caught in a channel handler:" + (!channel.isOpen() ? " (Closed Channel)" : ""), throwable);
        }
    }
}
