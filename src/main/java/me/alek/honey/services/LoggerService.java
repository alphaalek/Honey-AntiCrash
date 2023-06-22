package me.alek.honey.services;

import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.Bukkit;

import java.util.List;

public class LoggerService {

    private boolean enabled;
    private boolean console;
    private boolean detailed;
    private boolean truncateOld;
    private List<String> ignoredPlayers;
    private List<String> ignoredPackets;

    public LoggerService() {
        reload();
    }

    public enum LoggingStrategy {
        FLAG, VIOLATE, KICK
    }

    public void reload() {
        enabled = ConfigurationOptions.PacketLogger.ENABLED;
        console = ConfigurationOptions.PacketLogger.CONSOLE;
        detailed = ConfigurationOptions.PacketLogger.DETAILED;
        truncateOld = ConfigurationOptions.PacketLogger.TRUNCATE_OLD_LOGS;
        ignoredPlayers = ConfigurationOptions.PacketLogger.IGNORED_PLAYERS;
        ignoredPackets = ConfigurationOptions.PacketLogger.IGNORED_PACKETS;
    }

    public void log(
            LoggingStrategy strategy,
            Object handle,
            String playerName,
            String violationName,
            ViolationService.PacketLimitHandler packetLimitHandler,
            ConfigurationOptions.VlSetting setting
    ) {
        if (ignoredPlayers.contains(playerName)) {
            return;
        }
        if (ignoredPackets.contains(handle.getClass().getSimpleName())) {
            return;
        }
        if (strategy == LoggingStrategy.KICK) {
            Bukkit.getLogger().info("Kicking " + playerName + " for " + violationName);
            return;
        }
        String logType = (strategy == LoggingStrategy.FLAG) ? "flagged" : "violated";
        Bukkit.getLogger().info(
                 logType + " " + violationName + " " + packetLimitHandler.getFlagged() + "/" + setting.getMaxPerInterval() + ", "
                + packetLimitHandler.getViolations() + "/" + setting.getVlsAmount() + ", " + setting.getInterval()
                + " (" + handle.getClass().getSimpleName() + ")"
        );
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean useConsole() {
        return console;
    }

    public boolean isDetailed() {
        return detailed;
    }

    public boolean isTruncateOld() {
        return truncateOld;
    }

    public List<String> getIgnoredPlayers() {
        return ignoredPlayers;
    }

    public List<String> getIgnoredPackets() {
        return ignoredPackets;
    }
}
