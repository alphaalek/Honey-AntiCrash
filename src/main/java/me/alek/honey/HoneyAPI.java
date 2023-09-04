package me.alek.honey;

import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.Configuration;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.services.ConfiscationService;
import me.alek.honey.services.LoggerService;
import me.alek.honey.services.NotificationService;
import me.alek.honey.services.ViolationService;
import me.alek.honey.storage.sql.SQLManagerWrapper;
import me.alek.honey.tracker.MarkableHandleTracker;
import me.alek.honey.tracker.StatisticHandleTracker;
import me.alek.honey.utility.UpdateChecker;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public interface HoneyAPI {

    JavaPlugin getPlugin();

    void resetConfiguration();

    void reload();

    PacketLibrary getPacketLibrary();

    ViolationService getViolationService();

    NotificationService getNotificationService();

    ConfiscationService getConfiscationService();

    SQLManagerWrapper getSQLManager();

    LoggerService getLoggerService();

    StatisticHandleTracker<Double, Player> getNettyPacketHandleTracker();

    StatisticHandleTracker<Double, Player> getServerThreadHandleTracker();

    MarkableHandleTracker<Double, String> getCheckHandleTracker();

    Configuration getConfiguration();

    CommandManager getCommandManager();

    boolean isUsingOldVersion();

    UpdateChecker getUpdateChecker();

    boolean isRunning();

    int getRecentTPS();

    int getRecentPing(Player player);

    void removeRecentPingLookup(Player player);
}
