package me.alek.honey.validation.print;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.handler.InboundPacketFilter;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.tickloop.packet.TaskLifecycleHandler;
import me.alek.honey.validation.impl.NettyValidations;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
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

    public static synchronized org.apache.logging.log4j.core.Filter.Result handle(Throwable thrown, Supplier<Integer> length) {
        if (handling.get() != null) {
            return Filter.Result.NEUTRAL;
        }

        PacketContainer<? extends WrappedPacket<?>> packet = null;
        if (Thread.currentThread().getName().startsWith("Netty") || Thread.currentThread().getName().startsWith("honey-packet-decoder")) {
            packet = InboundPacketFilter.MARK.get();
        } else if (Thread.currentThread().getName().equals("Server Thread")) {
            packet = TaskLifecycleHandler.getCurrentHandledPacket();
        }
        if (packet == null) {
            return Filter.Result.NEUTRAL;
        }

        if (thrown != null) {
            try {
                SuppressedException ex = getSuppressedException(packet.getPlayer(), thrown);
                if (ex == null) {
                    suppressException(packet.getPlayer(), thrown);
                    return Filter.Result.NEUTRAL;
                } else {
                    ex.suppress(packet, thrown);
                }
            } catch (Exception ignored) {
                // ignored
            }
        }

        handling.set(true);

        Cancellable.Trigger cancellable = new Cancellable.Trigger();
        HoneyPlayer player = packet.getPlayer();
        int calculated = length.get();
        NettyValidations.PRINT_FILTER_PIPELINE.doStream(
                player,
                (validation, context) -> validation.validate(player, calculated, context),
                cancellable);

        handling.remove();

        return cancellable.isCancelled() ? Filter.Result.DENY : Filter.Result.NEUTRAL;
    }

    private static String stackTraceToUnit(Throwable thrown) {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(thrown.getStackTrace())
                .map(StackTraceElement::toString)
                .forEach(builder::append);

        return builder.toString();
    }

    public static SuppressedException getSuppressedException(HoneyPlayer player, Throwable thrown) throws ExecutionException {
        return player.suppressedExceptions.get(stackTraceToUnit(thrown), () -> null);
    }

    public static void suppressException(HoneyPlayer player, Throwable thrown) {
        player.suppressedExceptions.put(stackTraceToUnit(thrown), new SuppressedException(thrown));
    }
}
