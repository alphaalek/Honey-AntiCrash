package me.alek.honey;

import me.alek.honey.checks.Settings;
import me.alek.honey.checks.impl.NettyValidation;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.handler.InboundPacketFilter;
import me.alek.honey.netty.packet.Cancellable;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Enumeration;
import java.util.logging.*;

public class ErrorPrintFilter {

    public static void inject() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(HoneyPlugin.get().getPlugin(), () -> {
            Enumeration<String> loggerEnumerator = LogManager.getLogManager().getLoggerNames();
            while (loggerEnumerator.hasMoreElements()) {
                Logger logger = LogManager.getLogManager().getLogger(loggerEnumerator.nextElement());
                injectLogger(logger);
            }

            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                injectLogger(plugin.getLogger());
            }
        });
    }

    private static void injectLogger(Logger logger) {
        Filter prevFilter = Bukkit.getLogger().getFilter();
        logger.setFilter(getFilter(prevFilter));
    }

    private static Filter getFilter(Filter prevFilter) {
        return record -> {
            if (prevFilter != null && !prevFilter.isLoggable(record)) {
                return false;
            }

            if (!Thread.currentThread().getName().startsWith("Netty")
                    || record.getLevel() != Level.WARNING && record.getLevel() != Level.SEVERE) {
                return true;
            }

            PacketContainer<? extends WrappedPacket<?>> packet = InboundPacketFilter.MARK.get();
            if (packet == null) {
                return true;
            }

            int length = getLength(record);

            Cancellable.Stated stated = new Cancellable.Stated();
            NettyValidation.STATIC_INTS.doStream(packet.getPlayer(), (check, key) -> Settings.getSetting(check), check -> {
                try {
                    return check.validate(packet.getPlayer(), length, null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Bukkit.getLogger().severe("Failed to handle packet validation for packet " + packet);
                }
                return Validation.PASS;
            }, stated, null, null);

            Bukkit.broadcastMessage("Error log Length " + length + ", packet: " + packet.getPlayerName());

            return !stated.isCancelled();
        };
    }

    private static int getLength(LogRecord record) {
        int length = 0;
        length += record.getMessage().getBytes().length;

        if (record.getThrown() != null) {
            for (StackTraceElement trace : record.getThrown().getStackTrace()) {
                length += trace.toString().getBytes().length;
            }
        }

        return length;
    }
}
