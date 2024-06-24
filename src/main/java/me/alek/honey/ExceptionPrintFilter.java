package me.alek.honey;

import me.alek.honey.checks.impl.NettyValidation;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.handler.InboundPacketFilter;
import me.alek.honey.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Enumeration;
import java.util.logging.*;

public class ExceptionPrintFilter {

    public static void inject() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(HoneyPlugin.get().getPlugin(), () -> {
            Enumeration<String> loggerEnumerator = LogManager.getLogManager().getLoggerNames();
            while (loggerEnumerator.hasMoreElements()) {
                Logger logger = LogManager.getLogManager().getLogger(loggerEnumerator.nextElement());

                injectLogger(logger);

                Bukkit.broadcastMessage(logger + " " + logger.getFilter());
            }

            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                injectLogger(plugin.getLogger());

                Bukkit.broadcastMessage(plugin.getLogger() + " " + plugin.getLogger().getFilter());
            }

            Bukkit.broadcastMessage(Bukkit.getPluginManager().getPlugin("ViaVersion").getLogger() + " " + Bukkit.getPluginManager().getPlugin("ViaVersion").getLogger().getFilter());
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
            try {

                NettyValidation.STATIC_NETTY_A.check(packet.getPlayer(), length, null);

            } catch (Exception ex) {

            }

            Bukkit.broadcastMessage("Error log Length " + length + ", packet: " + packet.getPlayerName());

            return true;
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
