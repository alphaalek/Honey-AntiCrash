package me.alek.honey.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.ChannelCloseInspection;
import me.alek.honey.netty.packet.PacketContainer;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

public class HoneyDebugger extends ChannelInboundHandlerAdapter {

    private static final String VIA_VERSION_CANCEL = "This packet is supposed to be cancelled; If you have debug enabled, you can ignore these";
    private static final String NETTY_NATIVE_READ_RESET = "Error while read(...): Connection reset by peer";
    private static final String NETTY_NATIVE_WRITE_RESET = "Error while write(...): Connection reset by peer";
    private static final String NETTY_NATIVE_WRITE_BROKEN_PIPE = "Error while write(...): Broken pipe";
    private static final String NETTY_CONNECTION_RESET = "Connection reset";

    private final HoneyPlayer player;

    public HoneyDebugger(HoneyPlayer player) {
        this.player = player;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        onExceptionCaught(ctx.channel(), cause);
        try {
            ctx.fireExceptionCaught(cause);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        this.player.online = false;
        ChannelCloseInspection.close(ctx.channel());
    }

    public static void onExceptionCaught(Channel channel, Throwable throwable) {
        String message = Optional.ofNullable(throwable.getMessage()).orElse("");

        if (message.equals(VIA_VERSION_CANCEL)) {
            PacketContainer<?> packet = InboundPacketFilter.MARK.get();
            if (packet != null) {
                HoneyPlugin.get().getPacketLibrary().getChannelManager().getFilter(channel).handleViaDroppedPacket(packet);
            }
            InboundPacketFilter.removeMarker();
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
