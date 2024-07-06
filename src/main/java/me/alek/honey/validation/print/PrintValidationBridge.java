package me.alek.honey.validation.print;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.handler.InboundPacketFilter;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.packet.PacketTaskHandler;
import me.alek.honey.validation.impl.NettyValidations;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.bukkit.Bukkit;

import java.util.function.Supplier;

public class PrintValidationBridge {

    public static void inject() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(HoneyPlugin.get().getPlugin(), () -> {
            LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
            Configuration config = context.getConfiguration();

            LoggerConfig loggerConfig = config.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
            loggerConfig.addFilter(new Log4jFilter());

            context.updateLoggers();
        });
    }

    private static final ThreadLocal<Boolean> handling = new ThreadLocal<>();

    public static synchronized org.apache.logging.log4j.core.Filter.Result handle(Supplier<Integer> length) {
        if (handling.get() != null) {
            return Filter.Result.NEUTRAL;
        }

        PacketContainer<? extends WrappedPacket<?>> packet = null;
        if (Thread.currentThread().getName().startsWith("Netty")
                || Thread.currentThread().getName().startsWith("honey-packet-decoder")) {
            packet = InboundPacketFilter.MARK.get();
        } else if (Thread.currentThread().getName().equals("Server Thread")) {
            packet = PacketTaskHandler.getCurrentHandledPacket();
        }

        if (packet == null) {
            return org.apache.logging.log4j.core.Filter.Result.NEUTRAL;
        }

        handling.set(true);

        Cancellable.Stated stated = new Cancellable.Stated();
        HoneyPlayer player = packet.getPlayer();
        NettyValidations.PRINT_FILTER_PIPELINE.doStream(
                player,
                (validation, context) -> validation.validate(player, length.get(), context),
                stated);

        handling.remove();

        return stated.isCancelled() ? org.apache.logging.log4j.core.Filter.Result.DENY : org.apache.logging.log4j.core.Filter.Result.NEUTRAL;
    }
}
