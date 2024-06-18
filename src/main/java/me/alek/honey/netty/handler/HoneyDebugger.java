package me.alek.honey.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.util.UnsafeChannelCloser;

import java.io.IOException;
import java.util.Optional;

public class HoneyDebugger extends ChannelInboundHandlerAdapter {

    private static final String VIA_VERSION_CLOSED_CHANNEL = "This packet is supposed to be cancelled; If you have debug enabled, you can ignore these";
    private static final String NETTY_NATIVE_READ_RESET = "Error while read(...): Connection reset by peer";
    private static final String NETTY_NATIVE_WRITE_RESET = "Error while write(...): Connection reset by peer";
    private static final String NETTY_NATIVE_WRITE_BROKEN_PIPE = "Error while write(...): Broken pipe";
    private static final String NETTY_CONNECTION_RESET = "Connection reset";

    private volatile HoneyPlayer player;

    public HoneyDebugger() {

    }

    public HoneyPlayer getPlayer(){
        return player;
    }

    public void setPlayer(HoneyPlayer player) {
        this.player = player;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        onExceptionCaught(cause, !ctx.channel().isOpen());
        try {
            ctx.fireExceptionCaught(cause);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (player != null) {
            player.punished = true;
        }
        UnsafeChannelCloser.closeForcibly(ctx.channel());
    }

    public static void onExceptionCaught(Throwable throwable, boolean channelClosed) {
        String message = Optional.ofNullable(throwable.getMessage()).orElse("");
        if (HoneyPlugin.get().getLoggerService().isDebug()
                && /* Java */ !(throwable instanceof ReadTimeoutException)
                && /* Java */ !(throwable instanceof IOException)
                && /* Netty */ !message.equals(NETTY_NATIVE_READ_RESET)
                && /* Netty */ !message.equals(NETTY_NATIVE_WRITE_BROKEN_PIPE)
                && /* Netty */ !message.equals(NETTY_NATIVE_WRITE_RESET)
                && /* Netty */ !message.equals(NETTY_CONNECTION_RESET)
                && /* ViaVersion */ !message.equals(VIA_VERSION_CLOSED_CHANNEL)) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("DEBUG: Exception was caught in a channel handler:" + (channelClosed ? " (Closed Channel)" : ""));
            throwable.printStackTrace();
        }
    }
}
