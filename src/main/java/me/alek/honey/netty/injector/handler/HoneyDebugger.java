package me.alek.honey.netty.injector.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import me.alek.honey.HoneyPlugin;

import java.nio.channels.ClosedChannelException;
import java.util.Optional;

public class HoneyDebugger extends ChannelInboundHandlerAdapter {

    private static final String VIA_VERSION_CLOSED_CHANNEL = "This packet is supposed to be cancelled; If you have debug enabled, you can ignore these";
    private static final String NETTY_NATIVE_READ_ADDRESS = "Error while read(...): Connection reset by peer";

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        onExceptionCaught(cause);
        try {
            super.exceptionCaught(ctx, cause);
        } catch (Exception ex) {
            ex.printStackTrace();
            ctx.channel().close();
        }
    }

    public static void onExceptionCaught(Throwable cause) {
        String message = Optional.ofNullable(cause.getMessage()).orElse("");
        if (HoneyPlugin.get().getLoggerService().isDebug()
                && !(cause instanceof ClosedChannelException)
                && !(cause instanceof ReadTimeoutException)
                && /* ViaVersion */ !message.equals(VIA_VERSION_CLOSED_CHANNEL)
                && /* Netty */ !message.equals(NETTY_NATIVE_READ_ADDRESS)) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Exception was caught in a channel handler:");
            cause.printStackTrace();
        }
    }
}
