package me.alek.honey;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.alek.honey.async.DebugCommand;
import me.alek.honey.async.WorkerPool;
import me.alek.honey.command.CommandManager;
import me.alek.honey.compat.spark.SparkTPSProvider;
import me.alek.honey.configuration.Configuration;
import me.alek.honey.configuration.DedicatedConfiguration;
import me.alek.honey.lang.Lang;
import me.alek.honey.listeners.DispenserListener;
import me.alek.honey.listeners.PlayerQuitListener;
import me.alek.honey.listeners.TabCompleteListener;
import me.alek.honey.listeners.TeleportListener;
import me.alek.honey.monitor.MonitorManager;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.PacketLibrarySettings;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.packet.PacketExecutors;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.services.ConfiscationService;
import me.alek.honey.services.LoggerService;
import me.alek.honey.services.NotificationService;
import me.alek.honey.services.ViolationService;
import me.alek.honey.session.NoSessionEncryption;
import me.alek.honey.session.Session;
import me.alek.honey.sql.RepositoryCollection;
import me.alek.honey.sql.SQLManager;
import me.alek.honey.tickloop.ExecutionQueueProxy;
import me.alek.honey.tracker.CraftScheduledTPSTracker;
import me.alek.honey.tracker.KeyedHandleTracker;
import me.alek.honey.tracker.RegistryHandleTracker;
import me.alek.honey.tracker.temp.TempIntervalTrackerManager;
import me.alek.honey.util.LibraryVersion;
import me.alek.honey.util.metrics.Metrics;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.FieldAccessor;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;
import me.alek.honey.validation.BaseValidationContainer;
import me.alek.honey.validation.ValidationContainers;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.print.PrintValidationBridge;
import me.alek.honey.validation.types.HoneyValidation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Level;

public class HoneyPlugin implements HoneyAPI {

    private static HoneyPlugin INSTANCE;

    private ViolationService violationService;
    private NotificationService notificationService;
    private ConfiscationService confiscationService;
    private LoggerService loggerService;
    private CommandManager commandManager;
    private SQLManager sqlManager;
    private Configuration configuration;
    private ExecutionQueueProxy serverThreadTracker;
    private TempIntervalTrackerManager packetNettyTracker;
    private KeyedHandleTracker<Double, HoneyValidation<?>> validationHandleTracker;
    private CraftScheduledTPSTracker TPSTracker;
    private MonitorManager monitorManager;
    private boolean isLoaded;
    private boolean isRunning;
    private PacketLibrary packetLibrary;
    private Thread serverThread;

    private final Plugin plugin;
    private final String latestVersion;
    private final boolean isUsingOldVersion;

    public HoneyPlugin(Plugin plugin, String version, String latestVersion) {
        this.plugin = plugin;
        INSTANCE = this;

        plugin.saveDefaultConfig();
        Lang.load();

        Session.setup();
        try {
            Session.requestSession();
        } catch (Exception ex) {
            registerFailedHoneyLoadEvents();
            throw new RuntimeException(Lang.get("failed.load"), ex.getCause());
        }

        this.latestVersion = latestVersion != null ? latestVersion : verifyAndGetLatestVersion();
        if (new LibraryVersion(plugin.getDescription().getVersion()).isOlderThan(new LibraryVersion(this.latestVersion))) {
            isUsingOldVersion = true;
            try {
                PluginDescriptionFile descriptionFile = plugin.getDescription();
                FieldAccessor<String> versionField = Reflection.getField(descriptionFile.getClass(), "version", String.class);

                versionField.set(descriptionFile, version);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            isUsingOldVersion = false;
        }
    }

    private void registerFailedHoneyLoadEvents() {
        HandlerList handlerList = PlayerJoinEvent.getHandlerList();
        Plugin fakePlugin = new DummyPlugin();
        handlerList.register(new RegisteredListener(new Listener() {}, (listener, event) -> {
            if (!(event instanceof PlayerJoinEvent)) {
                return;
            }

            Player player = ((PlayerJoinEvent)event).getPlayer();
            if (player.isOp()) {
                player.sendMessage(Lang.get("failed.load.session.error"));
            }

            Channel channel = NMS.getChannelFromPlayer(player);
            if (channel == null) {
                return;
            }

            channel.pipeline().addBefore("packet_handler", "honey-invalid-session", new ChannelInboundHandlerAdapter() {

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    if (player.isOp() && msg.getClass().getSimpleName().equals("PacketPlayInChat")) {
                        String message = ReflectStructure.Provider.acquireStructure(msg.getClass())
                                .withType(String.class).withTarget(msg)
                                .readField(0);

                        if (message.startsWith("/honey")) {
                            player.sendMessage(Lang.get("failed.load.session.error"));
                            return;
                        }
                    }

                    ctx.fireChannelRead(msg);
                }
            });

        }, EventPriority.LOWEST, fakePlugin, true));
    }

    private static class InvalidLicenseException extends RuntimeException {

        @Override
        public String getMessage() {
            return Lang.get("invalid.license");
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
            Bukkit.getLogger().log(Level.SEVERE, Lang.get("license.connection.error"), ex);
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
            Bukkit.getLogger().log(Level.SEVERE, Lang.get("license.connection.error"), ex);
        }
        throw new InvalidLicenseException();
    }

    public void onEnable() {
        // check compatability and conflicts of runtime load or minecraft version
        String conflict = null;
        if (isLoaded || isRunning)
            conflict = Lang.get("conflict.runtime.load");
        else if (NetProtocol.getProtocol() == NetProtocol.UNKNOWN)
            conflict = Lang.get("conflict.unsupported.version");

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

                throw new RuntimeException(Lang.get("conflict.misconfiguration"));
            }
            return;
        }

        // setup packet library
        setupPacketLibrary();

        if (!Bukkit.getServer().getPluginManager().isPluginEnabled(plugin)) {
            plugin.getLogger().severe(Lang.get("conflict.packet.library.setup"));
            return;
        }

        // setup tps tracker
        if (!SparkTPSProvider.SparkValidator.VALIDATOR.find().get()) {
            TPSTracker = new CraftScheduledTPSTracker();
        }

        // setup monitoring
        monitorManager = new MonitorManager();

        // setup services
        notificationService = new NotificationService();
        loggerService = new LoggerService();
        violationService = new ViolationService();
        confiscationService = new ConfiscationService();

        // setup sql handler
        sqlManager = new SQLManager(new RepositoryCollection());

        // setup tracking
        serverThreadTracker = new ExecutionQueueProxy();
        serverThreadTracker.inject();
        packetNettyTracker = new TempIntervalTrackerManager();
        validationHandleTracker = new RegistryHandleTracker<>();

        // setup checks
        for (BaseValidationContainer validation : ValidationContainers.getValidations()) {
            validation.register(packetLibrary);
            validation.load();
        }
        PacketExecutors.register();
        ItemAnalysis.reload();
        PrintValidationBridge.inject();

        // setup commands
        commandManager = new CommandManager(plugin);
        ((JavaPlugin)plugin).getCommand("asyncdecoderdebug").setExecutor(new DebugCommand());

        // setup metrics
        new Metrics(plugin, 19028);

        // setup listeners
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new TeleportListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new DispenserListener(), plugin);
        TabCompleteListener tabCompleteListener = new TabCompleteListener();
        Bukkit.getPluginManager().registerEvents(tabCompleteListener, plugin);
        packetLibrary.addListeners(tabCompleteListener, ListenerPriority.HIGH, PacketTypes.Play.Server.TAB_COMPLETE);

        getLoggerService().getLogger().info(Lang.getFormatted("loaded", plugin.getDescription().getVersion()));
    }

    private void setupPacketLibrary() {
        if (packetLibrary == null) {
            packetLibrary = PacketLibrary.set(new PacketLibrarySettings(plugin, "honey-filter", "honey-connection", "honey-channel-init", "honey-decode-prepare", "honey-encode-prepare", "honey-debugger"));
        }
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
                getLoggerService().getLogger().log(Level.SEVERE, Lang.get("failed.shutdown.session.error"), ex);
            }

            getLoggerService().getLogger().info(Lang.get("unloaded"));
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
        Lang.load();

        for (BaseValidationContainer validation : ValidationContainers.getValidations()) {
            validation.load();
        }
        ItemAnalysis.reload();

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

        validationHandleTracker.clear();

        monitorManager.reload();
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

    public SQLManager getSQLManager() {
        return sqlManager;
    }

    public LoggerService getLoggerService() {
        return loggerService;
    }

    public TempIntervalTrackerManager getNettyPacketTracker() {
        return packetNettyTracker;
    }

    public ExecutionQueueProxy getExecutionQueueProxy() {
        return serverThreadTracker;
    }

    public KeyedHandleTracker<Double, HoneyValidation<?>> getValidationHandleTracker() {
        return validationHandleTracker;
    }

    public CraftScheduledTPSTracker getTPSTracker() {
        return TPSTracker;
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
}
