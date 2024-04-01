package me.alek.honey.netty.injector.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import org.bukkit.Bukkit;

import java.nio.channels.ClosedChannelException;
import java.util.Optional;

public class HoneyDebugger extends ChannelInboundHandlerAdapter {

    private static final String VIA_VERSION_CLOSED_CHANNEL = "This packet is supposed to be cancelled; If you have debug enabled, you can ignore these";
    private static final String NETTY_NATIVE_READ_ADDRESS = "Error while read(...): Connection reset by peer";
    private static final String NETTY_CONNECTION_RESET = "Connection reset";

    private volatile HoneyPlayer player;

    public HoneyDebugger() {

    }

    public HoneyDebugger(HoneyPlayer player) {
        this.player = player;
    }

    public HoneyPlayer getPlayer(){
        return player;
    }

    public void setPlayer(HoneyPlayer player) {
        this.player = player;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        onExceptionCaught(cause);
        try {
            super.exceptionCaught(ctx, cause);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (ctx.channel().isOpen()) {
            if (player != null) {
                player.punished = true;
            }
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
