package me.alek.honey;

import me.alek.honey.checks.HoneyCheckType;
import me.alek.honey.checks.IListener;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.ListenerRegistry;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.Configuration;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.configuration.DedicatedConfiguration;
import me.alek.honey.listeners.DispenserListener;
import me.alek.honey.listeners.PlayerJoinListener;
import me.alek.honey.listeners.TabCompleteListener;
import me.alek.honey.metrics.Metrics;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.PacketLibrarySettings;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.injector.DuplexChannelHandler;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packet.PacketBundleFilters;
import me.alek.honey.netty.packet.PacketBundleSaver;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.services.ConfiscationService;
import me.alek.honey.services.LoggerService;
import me.alek.honey.services.NotificationService;
import me.alek.honey.services.ViolationService;
import me.alek.honey.storage.BeanRepositoryCollection;
import me.alek.honey.storage.SQLManagerWrapper;
import me.alek.honey.tracker.*;
import me.alek.honey.utility.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HoneyPlugin implements HoneyAPI {

    private static HoneyPlugin INSTANCE;

    private ViolationService violationService;
    private NotificationService notificationService;
    private ConfiscationService confiscationService;
    private LoggerService loggerService;
    private CommandManager commandManager;
    private SQLManagerWrapper sqlManager;
    private Configuration configuration;
    private WideTempQueuedHandleTracker packetServerThreadHandleTimeTracker;
    private WideTempQueuedHandleTracker packetNettyHandleTracker;
    private MarkableHandleTracker<Double, String> checkHandleTracker;
    private Tickloop tickloop;
    private MonitorManager monitorManager;
    private boolean isLoaded;
    private boolean isRunning;
    private boolean usingOldVersion;
    private UpdateChecker updateChecker;
    private PacketLibrary packetLibrary;
    private final Map<UUID, Integer> recentPingLookup = new HashMap<>();
    private Thread serverThread;

    private final Plugin plugin;

    public HoneyPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void onEnable() {
        // check compatability and conflicts of runtime load or minecraft version
        String conflict = null;
        if (isLoaded || isRunning)
            conflict = "Honey has been reloaded during runtime! You SHOULDN'T do this. Stop that, and restart the server instead.";
        /*
        else if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17))
             conflict = "Honey is currently not supported by Minecraft Version 1.17+! But don't panic, it will for " +
                 "sure come in the future! :-)";
        */
        else if (NetProtocol.getProtocol() == NetProtocol.UNKNOWN)
            conflict = "Honey is currently not supported by the Minecraft Version your server is using.";

        if (conflict != null) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            throw new RuntimeException(conflict);
        }

        isRunning = true;
        isLoaded = true;
        INSTANCE = this;
        serverThread = Thread.currentThread();

        // setup configuration and check if it is misconfigured
        configuration = new DedicatedConfiguration();

        if (configuration.isMisconfigured()) {
            if (plugin.getServer().getPluginManager().isPluginEnabled(plugin)) {
                resetConfiguration();
                plugin.getServer().getPluginManager().disablePlugin(plugin);

                throw new RuntimeException("You have a misconfigured config.yml. The prior config file is renamed to old-config.yml, you just have to restart the server.");
            }
            return;
        }

        // setup packet library
        packetLibrary = PacketLibrary.set(new PacketLibrarySettings(plugin, false, "honey_packet_handler", "honey_packet_decoder"));

        if (!Bukkit.getServer().getPluginManager().isPluginEnabled(plugin)) {
            plugin.getLogger().severe("Aborting because the plugin was disabled during packet library setup.");
            return;
        }

        // setup tickloop
        tickloop = new Tickloop();

        // setup monitoring
        this.monitorManager = new MonitorManager();

        // setup services
        notificationService = new NotificationService();
        loggerService = new LoggerService();
        violationService = new ViolationService();
        confiscationService = new ConfiscationService();

        // setup sql handler
        sqlManager = new SQLManagerWrapper(new BeanRepositoryCollection());

        // setup trackers
        packetNettyHandleTracker = new WideTempQueuedHandleTracker();
        packetServerThreadHandleTimeTracker = new ServerThreadHandleTracker();
        checkHandleTracker = new RegistryMarkableHandleTracker<>(200L);

        // setup checks
        plugin.getServer().getScheduler().runTaskLater(plugin, this::registerChecks, 20L);
        ItemAnalysis.reload();

        // setup commands
        commandManager = new CommandManager(plugin);

        // setup metrics
        new Metrics(plugin, 19028);

        // update checker
        this.updateChecker = new UpdateChecker();
        this.usingOldVersion = updateChecker.isOldVersion();

        // setup listeners
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new DispenserListener(), plugin);
        packetLibrary.addListener(new TabCompleteListener(), ListenerPriority.HIGH, () -> PacketType.Play.Server.TAB_COMPLETE);

        // setup runnable getting the ping of players
        setupRecentPingLookupRunnable();
    }

    private void setupRecentPingLookupRunnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    synchronized (recentPingLookup) {
                        recentPingLookup.put(player.getUniqueId(), HoneyPlugin.get().getNotificationService().getPing(player));
                    }
                }
                if (!HoneyPlugin.get().isRunning())
                    this.cancel();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 60L);
    }

    public void onDisable() {
        isRunning = false;

        if (PacketLibrary.get() != null) PacketLibrary.get().disable();
    }

    public <WP extends WrappedPacket<WP>> void registerChecks() {
        for (IListener check : ListenerRegistry.getChecks()) {
            if (check instanceof SyncPacketAdapter) {
                ListenerPriority priority = check.getPriority();

                if (check.getCheckType() == HoneyCheckType.PACKET && check instanceof IPacketListener)
                    PacketLibrary.get().addListeners((SyncPacketAdapter<WP>) check, priority, ((IPacketListener)check).getPacketTypes());

                if (check.getCheckType() == HoneyCheckType.DECODE)
                    PacketLibrary.get().addDecodeListener((SyncPacketAdapter<?>) check, priority);

                check.load();
                continue;
            }
            PacketLibrary.get().getEventManager().addListener(check);
        }
    }

    public void resetConfiguration() {
        File oldFile = new File(plugin.getDataFolder(), "old-config.yml");
        if (oldFile.exists()) {
            try {
                Files.deleteIfExists(oldFile.toPath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        File file = new File(plugin.getDataFolder(), "config.yml");

        if (file.exists()) {
            file.renameTo(new File(plugin.getDataFolder(), "old-config.yml"));
        }
        plugin.saveDefaultConfig();
    }

    public void reload() {
        configuration.reload();
        for (IListener check : ListenerRegistry.getChecks())
            check.load();

        violationService.reload();
        loggerService.reload();
        confiscationService.reload();

        sqlManager.reload();
        if (sqlManager.isEnabled()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    sqlManager.preparePlayer(player);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        ItemAnalysis.reload();

        checkHandleTracker.clear();

        monitorManager.reload();

        ((ServerThreadHandleTracker)packetServerThreadHandleTimeTracker).reload();

        PacketBundleFilters.updateFilterAllPackets();
        PacketBundleSaver.reloadFilters();

        for (Map.Entry<UUID, PacketBundleSaver> bundleSaver : new ArrayList<>(DuplexChannelHandler.getPacketBundleSavers().entrySet())) {
            PacketBundleSaver newBundleSaver = new PacketBundleSaver();
            bundleSaver.getValue().transferTo(newBundleSaver);

            DuplexChannelHandler.getPacketBundleSavers().put(bundleSaver.getKey(), newBundleSaver);
        }
    }

    public boolean shouldExempt(Player player) {
        return tickloop.evaluateTPS(Timespan.TEN_SECOND) < ConfigurationOptions.Exempt.MIN_TPS ||
                getRecentPing(player) > ConfigurationOptions.Exempt.MAX_PING;
    }

    public static HoneyAPI get() {
        return INSTANCE;
    }

    public Plugin getPlugin() {
        return plugin;
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

    public SQLManagerWrapper getSQLManager() {
        return sqlManager;
    }

    public LoggerService getLoggerService() {
        return loggerService;
    }

    public WideTempQueuedHandleTracker getNettyPacketHandleTracker() {
        return packetNettyHandleTracker;
    }

    public WideTempQueuedHandleTracker getServerThreadHandleTracker() {
        return packetServerThreadHandleTimeTracker;
    }

    public MarkableHandleTracker<Double, String> getCheckHandleTracker() {
        return checkHandleTracker;
    }

    public Tickloop getTickloop() {
        return tickloop;
    }

    public MonitorManager getMonitorManager() {
        return monitorManager;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public boolean isUsingOldVersion() {
        return usingOldVersion;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public Thread getServerThread() {
        return serverThread;
    }

    public int getRecentPing(Player player) {
        synchronized (recentPingLookup) {
            return recentPingLookup.computeIfAbsent(player.getUniqueId(), (d) -> notificationService.getPing(player));
        }
    }

    public void removeRecentPingLookup(Player player) {
        synchronized (recentPingLookup) {
            recentPingLookup.remove(player.getUniqueId());
        }
    }
}
