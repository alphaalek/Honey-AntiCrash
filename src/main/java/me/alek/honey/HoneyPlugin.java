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
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.PacketExecutors;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.ReflectStructure;
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
import me.alek.honey.validation.ValidationContainerRegistry;
import me.alek.honey.validation.ViolationManagement;
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

    public static final String HONEY_CONNECTION_CHANNEL_NAME = "honey-connection";
    public static final String HONEY_CHANNEL_INITIALIZER_CHANNEL_NAME = "honey-channel-init";
    public static final String HONEY_DEBUGGER_CHANNEL_NAME = "honey-debugger";
    public static final String HONEY_DECODER_CHANNEL_NAME = "honey-decoder";
    public static final String HONEY_ENCODER_CHANNEL_NAME = "honey-encoder";
    public static final String HONEY_FILTER_CHANNEL_NAME = "honey-filter";

    private static HoneyPlugin INSTANCE;

    private ViolationManagement violationService;
    private Notifications notificationService;
    private HoneyLogger logger;
    private CommandManager commandManager;
    private SQLManager sqlManager;
    private Configuration configuration;
    private ExecutionQueueProxy executionQueueProxy;
    private TempIntervalTrackerManager packetNettyTracker;
    private KeyedHandleTracker<Double, HoneyValidation<?>> validationHandleTracker;
    private CraftScheduledTPSTracker TPSTracker;
    private MonitorManager monitorManager;
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
            this.isUsingOldVersion = true;
            try {
                PluginDescriptionFile descriptionFile = plugin.getDescription();
                FieldAccessor<String> versionField = Reflection.getField(descriptionFile.getClass(), "version", String.class);

                versionField.set(descriptionFile, version);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            this.isUsingOldVersion = false;
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
        this.plugin.saveDefaultConfig();

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
        // check compatibility and conflicts of runtime load or minecraft version
        String conflict = null;
        if (this.isRunning) {
            conflict = Lang.get("conflict.runtime.load");
        } else if (NetProtocol.getProtocol() == NetProtocol.UNKNOWN) {
            conflict = Lang.get("conflict.unsupported.version");
        }

        if (conflict != null) {
            Bukkit.getPluginManager().disablePlugin(this.plugin);
            throw new RuntimeException(conflict);
        }

        this.isRunning = true;
        this.serverThread = Thread.currentThread();

        // setup configuration and check if it is misconfigured
        this.plugin.saveDefaultConfig();
        this.configuration = new DedicatedConfiguration();
        if (this.configuration.isMisconfigured()) {
            if (this.plugin.getServer().getPluginManager().isPluginEnabled(this.plugin)) {
                resetConfiguration();
                this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);

                throw new RuntimeException(Lang.get("conflict.misconfiguration"));
            }

            return;
        }

        // setup packet library
        setupPacketLibrary();

        if (!Bukkit.getServer().getPluginManager().isPluginEnabled(this.plugin)) {
            this.plugin.getLogger().severe(Lang.get("conflict.packet.library.setup"));
            return;
        }

        // setup tps tracker
        if (!SparkTPSProvider.SparkValidator.VALIDATOR.find().get()) {
            this.TPSTracker = new CraftScheduledTPSTracker();
        }

        // setup monitoring
        this.monitorManager = new MonitorManager();

        // setup services
        this.notificationService = new Notifications();
        this.logger = new HoneyLogger();
        this.violationService = new ViolationManagement();

        // setup sql handler
        this.sqlManager = new SQLManager(new RepositoryCollection());

        // setup tracking
        this.executionQueueProxy = new ExecutionQueueProxy();
        this.executionQueueProxy.inject();
        this.packetNettyTracker = new TempIntervalTrackerManager();
        this.validationHandleTracker = new RegistryHandleTracker<>();

        // setup validations
        for (BaseValidationContainer validation : ValidationContainerRegistry.initValidations(packetLibrary)) {
            validation.load();
        }
        PacketExecutors.register();
        ItemAnalysis.reload();
        PrintValidationBridge.inject();

        // setup commands
        this.commandManager = new CommandManager(plugin);
        ((JavaPlugin)this.plugin).getCommand("asyncdecoderdebug").setExecutor(new DebugCommand());

        // setup metrics
        new Metrics(this.plugin, 19028);

        // setup listeners
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), this.plugin);
        Bukkit.getPluginManager().registerEvents(new TeleportListener(), this.plugin);
        Bukkit.getPluginManager().registerEvents(new DispenserListener(), this.plugin);

        TabCompleteListener tabCompleteListener = new TabCompleteListener();
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
            Bukkit.getPluginManager().registerEvents(tabCompleteListener, this.plugin);
        } else {
            this.packetLibrary.addListeners(tabCompleteListener, PacketContainer.PipelineState.OBJECT, ListenerPriority.HIGH, PacketTypes.Play.Server.TAB_COMPLETE);
        }

        getLogger().getLogger().info(Lang.getFormatted("loaded", this.plugin.getDescription().getVersion()));
    }

    private void setupPacketLibrary() {
        if (this.packetLibrary == null) {
            this.packetLibrary = PacketLibrary.set(
                    new PacketLibrarySettings(this.plugin,
                            HONEY_FILTER_CHANNEL_NAME,
                            HONEY_CONNECTION_CHANNEL_NAME,
                            HONEY_CHANNEL_INITIALIZER_CHANNEL_NAME,
                            HONEY_DECODER_CHANNEL_NAME,
                            HONEY_ENCODER_CHANNEL_NAME,
                            HONEY_DEBUGGER_CHANNEL_NAME)
            );
        }
    }

    public void onDisable() {
        if (this.isRunning) {
            this.isRunning = false;

            getPacketLibrary().disable();

            WorkerPool.ASYNC_PACKET_DECODER_POOL.terminate();

            try {
                Session.sessionDecrypterClass.getDeclaredMethod("checkShutdown", boolean.class, boolean.class)
                        .invoke(null, false, true);
            } catch (Exception ex) {
                getLogger().getLogger().log(Level.SEVERE, Lang.get("failed.shutdown.session.error"), ex);
            }

            getLogger().getLogger().info(Lang.get("unloaded"));
        }
    }

    public void resetConfiguration() {
        File oldFile = new File(this.plugin.getDataFolder(), "old-config.yml");
        if (oldFile.exists()) {
            try {
                Files.deleteIfExists(oldFile.toPath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        File file = new File(this.plugin.getDataFolder(), "config.yml");

        if (file.exists()) {
            file.renameTo(new File(this.plugin.getDataFolder(), "old-config.yml"));
        }
        this.plugin.saveDefaultConfig();
    }

    public void reload() {
        this.configuration.reload();
        Lang.load();

        for (BaseValidationContainer validation : ValidationContainerRegistry.getValidations()) {
            validation.load();
        }
        ItemAnalysis.reload();

        this.violationService.reload();
        this.logger.reload();
        this.notificationService.reload();

        this.sqlManager.reload();
        if (this.sqlManager.isEnabled()) {
            for (HoneyPlayer player : HoneyPlayer.getAllPlayers()) {
                try {
                    this.sqlManager.preparePlayer(player);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        this.validationHandleTracker.clear();

        this.monitorManager.reload();
    }

    @NoSessionEncryption
    @Override
    public String getLicense() {
        return this.plugin.getConfig().getString("license", "");
    }

    public static HoneyAPI get() {
        return INSTANCE;
    }

    @Override
    public void sync(Runnable task) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, task);
    }

    @Override
    public void async(Runnable task) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, task);
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    public PacketLibrary getPacketLibrary() {
        if (this.packetLibrary == null) {
            this.packetLibrary = PacketLibrary.get();
        }

        return this.packetLibrary;
    }

    public ViolationManagement getViolationService() {
        return this.violationService;
    }

    public Notifications getNotificationService() {
        return this.notificationService;
    }

    public SQLManager getSQLManager() {
        return this.sqlManager;
    }

    public HoneyLogger getLogger() {
        return this.logger;
    }

    public TempIntervalTrackerManager getNettyPacketTracker() {
        return this.packetNettyTracker;
    }

    public ExecutionQueueProxy getExecutionQueueProxy() {
        return this.executionQueueProxy;
    }

    public KeyedHandleTracker<Double, HoneyValidation<?>> getValidationHandleTracker() {
        return this.validationHandleTracker;
    }

    public CraftScheduledTPSTracker getTPSTracker() {
        return this.TPSTracker;
    }

    public MonitorManager getMonitorManager() {
        return this.monitorManager;
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public String getLatestVersion() {
        return this.latestVersion;
    }

    public boolean isUsingOldVersion(){
        return this.isUsingOldVersion;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public Thread getServerThread() {
        return this.serverThread;
    }
}
