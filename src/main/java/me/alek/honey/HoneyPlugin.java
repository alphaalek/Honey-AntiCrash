package me.alek.honey;

import me.alek.honey.checks.HoneyCheckType;
import me.alek.honey.checks.IListener;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.ListenerRegistry;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.command.CommandManager;
import me.alek.honey.configuration.Configuration;
import me.alek.honey.configuration.DedicatedConfiguration;
import me.alek.honey.listeners.DispenserListener;
import me.alek.honey.listeners.PlayerJoinListener;
import me.alek.honey.listeners.TabCompleteListener;
import me.alek.honey.metrics.Metrics;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.PacketLibrarySettings;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.injector.handler.async.*;
import me.alek.honey.netty.listener.PacketAdapter;
import me.alek.honey.netty.packet.PacketExecutors;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.bundle.PacketBundleSaver;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.util.FieldAccessor;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.netty.util.Reflection;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.services.ConfiscationService;
import me.alek.honey.services.LoggerService;
import me.alek.honey.services.NotificationService;
import me.alek.honey.services.ViolationService;
import me.alek.honey.session.NoSessionEncryption;
import me.alek.honey.session.Session;
import me.alek.honey.storage.BeanRepositoryCollection;
import me.alek.honey.storage.SQLManagerWrapper;
import me.alek.honey.tracker.MarkableHandleTracker;
import me.alek.honey.tracker.RegistryMarkableHandleTracker;
import me.alek.honey.tracker.ServerThreadHandleTracker;
import me.alek.honey.tracker.Tickloop;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
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
    private WideTempQueuedHandleTracker serverThreadTracker;
    private WideTempQueuedHandleTracker packetNettyTracker;
    private MarkableHandleTracker<Double, String> checkHandleTracker;
    private Tickloop tickloop;
    private MonitorManager monitorManager;
    private boolean isLoaded;
    private boolean isRunning;
    private PacketLibrary packetLibrary;
    private final Map<UUID, Integer> recentPingLookup = new HashMap<>();
    private Thread serverThread;

    private final Plugin plugin;
    private final String latestVersion;
    private final boolean isUsingOldVersion;

    public HoneyPlugin(Plugin plugin, String version, String latestVersion) {
        this.plugin = plugin;
        INSTANCE = this;

        plugin.saveDefaultConfig();
        Session.setup();
        try {
            Session.requestSession();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load Honey", ex.getCause());
        }

        this.latestVersion = latestVersion != null ? latestVersion : verifyAndGetLatestVersion();
        isUsingOldVersion = !version.equals(this.latestVersion);

        if (!plugin.getDescription().getVersion().equals(version)) {
            try {
                PluginDescriptionFile descriptionFile = plugin.getDescription();
                FieldAccessor<String> versionField = Reflection.getField(descriptionFile.getClass(), "version", String.class);

                versionField.set(descriptionFile, version);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static class InvalidLicenseException extends RuntimeException {

        @Override
        public String getMessage() {
            return "You have an invalid license specified in your config.yml. Open the configuration file at /plugins/honey/config.yml and follow the instructions at the top.";
        }
    }

    private HttpsURLConnection getConnection() {
        plugin.saveDefaultConfig();

        if (getLicense().isEmpty()) {
            throw new InvalidLicenseException();
        }
        String urlString = "https://api.honey-license.workers.dev/verify-license?license=" + getLicense();

        try {
            URL url = new URL(urlString);

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0) Gecko/20100101 Firefox/10.0");

            return connection;
        } catch (IOException ex) {
            ex.printStackTrace();
            Bukkit.getLogger().severe("An error occurred when connecting to license auth servers!");
        }

        return null;
    }

    private String verifyAndGetLatestVersion() {
        HttpsURLConnection connection = getConnection();

        try {
            if (connection != null && connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return new BufferedReader(
                        new InputStreamReader(connection.getInputStream())).readLine();
            }
            Bukkit.getLogger().info(new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine());

        } catch (Exception ex) {
            ex.printStackTrace();
            Bukkit.getLogger().severe("An error occurred when connecting to license auth servers!");
        }
        throw new InvalidLicenseException();
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
        serverThread = Thread.currentThread();

        // setup configuration and check if it is misconfigured
        plugin.saveDefaultConfig();
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
        setupPacketLibrary();

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
        packetNettyTracker = new WideTempQueuedHandleTracker();
        serverThreadTracker = new ServerThreadHandleTracker();
        checkHandleTracker = new RegistryMarkableHandleTracker<>();

        // setup checks
        plugin.getServer().getScheduler().runTask(plugin, this::registerChecks); // register checks on the next tick (when the server has finished loading)
        PacketExecutors.register();
        ItemAnalysis.reload();

        // setup commands
        commandManager = new CommandManager(plugin);
        ((JavaPlugin)plugin).getCommand("asyncdecoderdebug").setExecutor(new DebugCommand());

        // setup metrics
        new Metrics(plugin, 19028);

        // setup listeners
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new DispenserListener(), plugin);
        packetLibrary.addListener(new TabCompleteListener(), ListenerPriority.HIGH, () -> PacketType.Play.Server.TAB_COMPLETE);

        // setup runnables
        setupRecentPingLookupRunnable();

        getLoggerService().getLogger().info("Honey is now fully loaded in! Version: " + plugin.getDescription().getVersion());
    }

    private void setupPacketLibrary() {
        if (packetLibrary == null) {
            packetLibrary = PacketLibrary.set(new PacketLibrarySettings(plugin, "honey-filter", "honey-connection", "honey-channel-init", "honey-decode-prepare", "honey-encode-prepare", "honey-debugger"));
        }
    }

    private void setupRecentPingLookupRunnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (recentPingLookup) {
                    for (HoneyPlayer player : HoneyPlayer.getAllPlayers()) {
                        recentPingLookup.put(player.uuid, HoneyPlugin.get().getNotificationService().getPing(player));
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 60L);
    }

    public void onDisable() {
        if (isRunning) {
            isRunning = false;

            getPacketLibrary().disable();

            WorkerPool.ASYNC_PACKET_DECODER_POOL.terminate();

            try {
                Session.sessionDecrypterClass.getDeclaredMethod("checkShutdown", boolean.class, boolean.class)
                        .invoke(null, false, true);
            } catch (Exception ex) {
                ex.printStackTrace();
                getLoggerService().getLogger().severe("Failed to shutdown session!");
            }

            getLoggerService().getLogger().info("Honey is now disabled!");
        }
    }

    public <WP extends WrappedPacket<WP>> void registerChecks() {
        for (IListener check : ListenerRegistry.getChecks()) {

            if (check instanceof PacketAdapter) {
                ListenerPriority priority = check.getPriority();
                if (check.getCheckType() == HoneyCheckType.PACKET && check instanceof IPacketListener)
                    packetLibrary.addListeners((PacketAdapter<WP>) check, priority, ((IPacketListener)check).getPacketTypes());
                if (check.getCheckType() == HoneyCheckType.DECODE)
                    packetLibrary.addDecodeListener((PacketAdapter<?>) check, priority);

                check.load();
            }
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
        notificationService.reload();
        confiscationService.reload();

        sqlManager.reload();
        if (sqlManager.isEnabled()) {
            for (HoneyPlayer player : HoneyPlayer.getAllPlayers()) {
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

        ((ServerThreadHandleTracker) serverThreadTracker).reload();

        PacketBundleFilters.updateFilterAllPackets();
        PacketBundleSaver.reloadFilters();

        for (HoneyPlayer player : HoneyPlayer.getAllPlayers()) {
            player.packetBundleSaver = new PacketBundleSaver();
        }
    }

    @NoSessionEncryption
    @Override
    public String getLicense() {
        return plugin.getConfig().getString("license", "");
    }

    public static HoneyAPI get() {
        return INSTANCE;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public PacketLibrary getPacketLibrary() {
        if (packetLibrary == null) {
            packetLibrary = PacketLibrary.get();
        }

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

    public WideTempQueuedHandleTracker getNettyPacketTracker() {
        return packetNettyTracker;
    }

    public WideTempQueuedHandleTracker getServerThreadTracker() {
        return serverThreadTracker;
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

    public String getLatestVersion() {
        return latestVersion;
    }

    public boolean isUsingOldVersion(){
        return isUsingOldVersion;
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

    public int getRecentPing(HoneyPlayer player) {
        synchronized (recentPingLookup) {
            return recentPingLookup.computeIfAbsent(player.uuid, (d) -> notificationService.getPing(player));
        }
    }

    public void removeRecentPingLookup(HoneyPlayer player) {
        synchronized (recentPingLookup) {
            recentPingLookup.remove(player.uuid);
        }
    }
}
