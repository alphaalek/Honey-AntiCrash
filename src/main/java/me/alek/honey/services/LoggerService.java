package me.alek.honey.services;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.protocol.Protocol;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class LoggerService {

    private boolean enabled;
    private boolean console;
    private boolean detailed;
    private boolean debug;
    private boolean truncateOld;
    private List<String> ignoredPlayers;
    private List<String> ignoredPackets;

    private final FuzzyPacketAdapter packetLoggerListener = new PacketLogger();

    public class PacketLogger extends FuzzyPacketAdapter {

        @Override
        @SuppressWarnings("unchecked")
        public void onPacketReceive(Player player, PacketContainer packetContainer) {
            logPacket(packetContainer, player.getName());
        }
    }

    public LoggerService() {
        reload();

        if (truncateOld) {
            // slet gamle filer her
        }
    }

    public enum LoggingStrategy {
        FLAG, VIOLATE
    }

    public void reload() {
        enabled = ConfigurationOptions.PacketLogger.ENABLED;
        console = ConfigurationOptions.PacketLogger.CONSOLE;
        detailed = ConfigurationOptions.PacketLogger.DETAILED;
        debug = ConfigurationOptions.PacketLogger.DEBUG;
        truncateOld = ConfigurationOptions.PacketLogger.TRUNCATE_OLD_LOGS;
        ignoredPlayers = ConfigurationOptions.PacketLogger.IGNORED_PLAYERS;
        ignoredPackets = ConfigurationOptions.PacketLogger.IGNORED_PACKETS;

        boolean isRegistered = HoneyPlugin.get().getPacketLibrary().isFuzzyRegistered(packetLoggerListener);
        if (enabled && !isRegistered) {
            HoneyPlugin.get().getPacketLibrary().addFuzzyListeners(packetLoggerListener, ListenerPriority.HIGHEST, () -> PacketType.getTypesAvailableFor(PacketState.PLAY, PacketBound.CLIENT, Protocol.getProtocol()));
        }
        if (!enabled && isRegistered) {
            HoneyPlugin.get().getPacketLibrary().removeFuzzyListeners(packetLoggerListener, PacketType.getTypesAvailableFor(PacketState.PLAY, PacketBound.CLIENT, Protocol.getProtocol()));
        }
    }

    public void logDebug(
            LoggingStrategy strategy,
            Object handle,
            String playerName,
            String violationName,
            ViolationService.AbstractPacketLimitHandler packetLimitHandler,
            ConfigurationOptions.VlSetting setting
    ) {
        if (!debug) return;
        if (ignoredPlayers.contains(playerName) || ignoredPackets.contains(handle.getClass().getSimpleName())) {
            return;
        }
        String logType = (strategy == LoggingStrategy.FLAG) ? "flagged" : "violated";
        Bukkit.getLogger().info(
                 logType + " " + playerName + " for " + violationName + ". " + ChatColor.stripColor(HoneyPlugin.get().getNotificationService().formatNotification(setting, packetLimitHandler))
                + " (" + handle.getClass().getSimpleName() + ")"
        );
    }

    public <WP extends WrappedPacket<WP>> void logPacket(
            PacketContainer<WP> packetContainer,
            String playerName
    ) {
        if (ignoredPlayers.contains(playerName) || ignoredPackets.contains(packetContainer.getHandle().getClass().getSimpleName())) {
            return;
        }
        StringBuilder message = new StringBuilder()
                .append(playerName)
                .append(" sendte ")
                .append(packetContainer.getHandle().getClass().getSimpleName());
        if (detailed) {
            message
                .append(": ")
                .append(packetContainer.getAll().readFieldValues());
        }

        if (console) {
            Bukkit.getLogger().info(message.toString());
        }
        // log i fil her
    }

    public void logPunish(String playerName, String violationName) {
        if (!debug) return;
        Bukkit.getLogger().info("Kicking " + playerName + " for " + violationName);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean useConsole() {
        return console;
    }

    public boolean isDebug() {
        return debug;
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
