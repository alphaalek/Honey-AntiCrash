package me.alek.honey;

import me.alek.honey.checks.*;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.command.CommandManager;
import me.alek.honey.command.impl.HelpCommand;
import me.alek.honey.command.impl.ReloadCommand;
import me.alek.honey.configuration.Configuration;
import me.alek.honey.configuration.DedicatedConfiguration;
import me.alek.honey.listeners.PlayerJoinListener;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.PacketLibrarySettings;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.services.ConfiscationService;
import me.alek.honey.services.LoggerService;
import me.alek.honey.services.NotificationService;
import me.alek.honey.services.ViolationService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class HoneyPlugin extends JavaPlugin {

    private static HoneyPlugin INSTANCE;

    private ViolationService violationService;
    private NotificationService notificationService;
    private ConfiscationService confiscationService;
    private LoggerService loggerService;
    private CommandManager commandManager;
    private Configuration configuration;
    private boolean isRunning;
    private PacketLibrary packetLibrary;

    @Override
    public void onEnable() {
        isRunning = true;
        INSTANCE = this;

        packetLibrary = PacketLibrary.set(new PacketLibrarySettings(this, false, "alek_packets"));

        configuration = new DedicatedConfiguration();

        loggerService = new LoggerService();
        violationService = new ViolationService();
        notificationService = new NotificationService();
        confiscationService = new ConfiscationService();

        commandManager = new CommandManager(this);
        new ReloadCommand(commandManager);
        new HelpCommand(commandManager);

        registerChecks();
        ItemAnalysis.reload();

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
    }

    @Override
    public void onDisable() {
        isRunning = false;

        PacketLibrary.get().disable();
    }

    public <WP extends WrappedPacket<WP>> void registerChecks() {
        for (IListener check : ListenerRegistry.getChecks()) {

            if (check instanceof IPacketListener && check instanceof SyncPacketAdapter) {
                ListenerPriority priority;
                if (check instanceof AbstractPacketListener) {
                    priority = ((AbstractPacketListener<WP>)check).getPriority();
                }
                else {
                    priority = ListenerPriority.NORMAL;
                }
                PacketLibrary.get().addListeners((SyncPacketAdapter<WP>) check, priority, ((IPacketListener)check).getPacketTypes());
                check.load();
                continue;
            }
            PacketLibrary.get().getEventManager().addListener(check);
        }
    }

    public void reload() {
        configuration.reload();
        for (IListener check : ListenerRegistry.getChecks()) {
            check.load();
        }
        violationService.reload();
        loggerService.reload();
        ItemAnalysis.reload();
    }

    public static HoneyPlugin get() {
        return INSTANCE;
    }

    public PacketLibrary getPacketLibrary() {
        return packetLibrary;
    }

    public ViolationService getViolationService() {
        return violationService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public ConfiscationService getConfiscationService() {
        return confiscationService;
    }

    public LoggerService getLoggerService() {
        return loggerService;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
}
