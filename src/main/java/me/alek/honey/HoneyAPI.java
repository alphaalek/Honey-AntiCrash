package me.alek.honey;

import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.Configuration;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.services.ConfiscationService;
import me.alek.honey.services.LoggerService;
import me.alek.honey.services.NotificationService;
import me.alek.honey.services.ViolationService;
import me.alek.honey.storage.SQLManagerWrapper;
import me.alek.honey.tracker.*;
import me.alek.honey.utility.UpdateChecker;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface HoneyAPI {

    Plugin getPlugin();

    void resetConfiguration();

    void reload();

    boolean shouldExempt(Player player);

    PacketLibrary getPacketLibrary();

    ViolationService getViolationService();

    NotificationService getNotificationService();

    ConfiscationService getConfiscationService();

    SQLManagerWrapper getSQLManager();

    LoggerService getLoggerService();

    WideTempQueuedHandleTracker getNettyPacketHandleTracker();

    WideTempQueuedHandleTracker getServerThreadHandleTracker();

    MarkableHandleTracker<Double, String> getCheckHandleTracker();

    Tickloop getTickloop();

    MonitorManager getMonitorManager();

    Configuration getConfiguration();

    CommandManager getCommandManager();

    boolean isUsingOldVersion();

    UpdateChecker getUpdateChecker();

    boolean isRunning();

    boolean isLoaded();

    Thread getServerThread();

    int getRecentPing(Player player);

    void removeRecentPingLookup(Player player);

    void onEnable();

    void onDisable();

    QueueExecutionWatchdogThread getWatchdog();
}
