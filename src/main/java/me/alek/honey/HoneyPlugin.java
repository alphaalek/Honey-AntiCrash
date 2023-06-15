package me.alek.honey;

import me.alek.honey.checks.Check;
import me.alek.honey.checks.CheckRegistry;
import me.alek.honey.checks.PacketCheck;
import me.alek.honey.configuration.Configuration;
import me.alek.honey.configuration.DedicatedConfiguration;
import me.alek.honey.listeners.PlayerJoinListener;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.PacketLibrarySettings;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.services.ConfiscationService;
import me.alek.honey.services.NotificationService;
import me.alek.honey.services.ViolationService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class HoneyPlugin extends JavaPlugin {

    private static HoneyPlugin INSTANCE;

    private ViolationService violationService;
    private NotificationService notificationService;
    private ConfiscationService confiscationService;
    private Configuration configuration;
    private boolean isRunning;
    private PacketLibrary packetLibrary;

    @Override
    public void onEnable() {
        isRunning = true;
        INSTANCE = this;

        packetLibrary = PacketLibrary.set(new PacketLibrarySettings(this, false, "alek_packets"));

        configuration = new DedicatedConfiguration();
        violationService = new ViolationService();
        notificationService = new NotificationService();
        confiscationService = new ConfiscationService();

        registerChecks();

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
    }

    @Override
    public void onDisable() {
        isRunning = false;

        PacketLibrary.get().disable();
    }

    public <WP extends WrappedPacket<WP>> void registerChecks() {
        for (Check check : CheckRegistry.getChecks()) {

            if (check instanceof PacketCheck && check instanceof AsyncPacketAdapter) {
                PacketLibrary.get().addListeners((AsyncPacketAdapter<WP>) check, ((PacketCheck)check).getPacketTypes());
                check.load();
                continue;
            }
            PacketLibrary.get().getEventManager().addListener(check);
        }
    }

    public void reload() {
        for (Check check : CheckRegistry.getChecks()) {
            check.load();
        }

        // idk
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

    public Configuration getConfiguration() {
        return configuration;
    }
}
