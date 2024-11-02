package me.alek.honey;

import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.Configuration;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.validation.ViolationManagement;
import me.alek.honey.sql.SQLManager;
import me.alek.honey.tickloop.ExecutionQueueProxy;
import me.alek.honey.tracker.CraftScheduledTPSTracker;
import me.alek.honey.tracker.KeyedHandleTracker;
import me.alek.honey.tracker.temp.TempIntervalTrackerManager;
import me.alek.honey.validation.types.HoneyValidation;
import org.bukkit.plugin.Plugin;

public interface HoneyAPI {

    void sync(Runnable task);

    void async(Runnable task);

    Plugin getPlugin();

    void resetConfiguration();

    void reload();

    String getLicense();

    PacketLibrary getPacketLibrary();

    ViolationManagement getViolationService();

    Notifications getNotificationService();

    SQLManager getSQLManager();

    HoneyLogger getLogger();

    TempIntervalTrackerManager getNettyPacketTracker();

    ExecutionQueueProxy getExecutionQueueProxy();

    KeyedHandleTracker<Double, HoneyValidation<?>> getValidationHandleTracker();

    CraftScheduledTPSTracker getTPSTracker();

    MonitorManager getMonitorManager();

    Configuration getConfiguration();

    CommandManager getCommandManager();

    boolean isUsingOldVersion();

    String getLatestVersion();

    boolean isRunning();

    Thread getServerThread();

    void onEnable();

    void onDisable();
}
