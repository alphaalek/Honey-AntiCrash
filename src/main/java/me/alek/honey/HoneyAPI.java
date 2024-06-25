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
import me.alek.honey.tickloop.ExecutionQueueProxy;
import me.alek.honey.tracker.CraftScheduledTPSTracker;
import me.alek.honey.tracker.MarkableHandleTracker;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import org.bukkit.plugin.Plugin;

public interface HoneyAPI {

    Plugin getPlugin();

    void resetConfiguration();

    void reload();

    String getLicense();

    PacketLibrary getPacketLibrary();

    ViolationService getViolationService();

    NotificationService getNotificationService();

    ConfiscationService getConfiscationService();

    SQLManagerWrapper getSQLManager();

    LoggerService getLoggerService();

    WideTempQueuedHandleTracker getNettyPacketTracker();

    ExecutionQueueProxy getExecutionQueueProxy();

    MarkableHandleTracker<Double, String> getValidationHandleTracker();

    CraftScheduledTPSTracker getTPSTracker();

    MonitorManager getMonitorManager();

    Configuration getConfiguration();

    CommandManager getCommandManager();

    boolean isUsingOldVersion();

    String getLatestVersion();

    boolean isRunning();

    boolean isLoaded();

    Thread getServerThread();

    void onEnable();

    void onDisable();
}
