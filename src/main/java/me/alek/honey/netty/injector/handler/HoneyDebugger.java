package me.alek.honey.netty.injector.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import me.alek.honey.HoneyPlugin;
import org.bukkit.Bukkit;

import java.nio.channels.ClosedChannelException;
import java.util.Optional;

@ChannelHandler.Sharable
public class HoneyDebugger extends ChannelInboundHandlerAdapter {

    public static final HoneyDebugger DEBUGGER = new HoneyDebugger();

    private static final String VIA_VERSION_CLOSED_CHANNEL = "This packet is supposed to be cancelled; If you have debug enabled, you can ignore these";
    private static final String NETTY_NATIVE_READ_ADDRESS = "Error while read(...): Connection reset by peer";
    private static final String NETTY_CONNECTION_RESET = "Connection reset";

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

    public static void onExceptionCaught(Throwable throwable) {
        String message = Optional.ofNullable(throwable.getMessage()).orElse("");
        if (HoneyPlugin.get().getLoggerService().isDebug()
                && !(throwable instanceof ClosedChannelException)
                && !(throwable instanceof ReadTimeoutException)
                && /* ViaVersion */ !message.equals(VIA_VERSION_CLOSED_CHANNEL)
                && /* Netty */ !message.equals(NETTY_NATIVE_READ_ADDRESS)
                && /* Netty */ !message.equals(NETTY_CONNECTION_RESET)) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Exception was caught in a channel handler:");
            throwable.printStackTrace();
        }
    }
}
