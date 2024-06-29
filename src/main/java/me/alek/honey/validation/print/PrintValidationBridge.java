package me.alek.honey.validation.print;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.handler.InboundPacketFilter;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.packetwrappers.WrappedPacket;
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

        if (!Thread.currentThread().getName().startsWith("Netty")) {
            return org.apache.logging.log4j.core.Filter.Result.NEUTRAL;
        }

        PacketContainer<? extends WrappedPacket<?>> packet = InboundPacketFilter.MARK.get();
        if (packet == null) {
            return org.apache.logging.log4j.core.Filter.Result.NEUTRAL;
        }

        handling.set(true);

        Cancellable.Stated stated = new Cancellable.Stated();
        NettyValidations.PRINT_FILTER_PIPELINE.doStream(
                packet.getPlayer(),
                (validation, container) -> validation.validate(packet.getPlayer(), length.get(), container),
                stated);

        handling.remove();

        return stated.isCancelled() ? org.apache.logging.log4j.core.Filter.Result.DENY : org.apache.logging.log4j.core.Filter.Result.NEUTRAL;
    }
}
