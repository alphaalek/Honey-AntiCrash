package me.alek.honey;

import me.alek.honey.checks.HoneyCheckType;
import me.alek.honey.checks.IListener;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.ListenerRegistry;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.command.CommandManager;
import me.alek.honey.command.impl.*;
import me.alek.honey.configuration.Configuration;
import me.alek.honey.configuration.DedicatedConfiguration;
import me.alek.honey.listeners.DispenserListener;
import me.alek.honey.listeners.PlayerJoinListener;
import me.alek.honey.metrics.Metrics;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.PacketLibrarySettings;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.services.ConfiscationService;
import me.alek.honey.services.LoggerService;
import me.alek.honey.services.NotificationService;
import me.alek.honey.services.ViolationService;
import me.alek.honey.storage.sql.BeanRepositoryCollection;
import me.alek.honey.storage.sql.SQLManagerWrapper;
import me.alek.honey.tracker.StatisticHandleTracker;
import me.alek.honey.tracker.impl.CheckHandleTracker;
import me.alek.honey.tracker.impl.PlayerNettyPacketHandleTracker;
import me.alek.honey.tracker.MarkableHandleTracker;
import me.alek.honey.tracker.impl.PlayerServerThreadHandleTracker;
import me.alek.honey.utility.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HoneyPlugin extends JavaPlugin implements HoneyAPI {

    private static HoneyPlugin INSTANCE;

    private ViolationService violationService;
    private NotificationService notificationService;
    private ConfiscationService confiscationService;
    private LoggerService loggerService;
    private CommandManager commandManager;
    private SQLManagerWrapper sqlManager;
    private Configuration configuration;
    private StatisticHandleTracker<Double, Player> packetServerThreadHandleTimeTracker;
    private StatisticHandleTracker<Double, Player> packetNettyHandleTracker;
    private MarkableHandleTracker<Double, String> checkHandleTracker;
    private boolean isRunning;
    private boolean usingOldVersion;
    private UpdateChecker updateChecker;
    private PacketLibrary packetLibrary;
    private int recentTPS = 20;
    private final Map<UUID, Integer> recentPingLookup = new HashMap<>();

    @Override
    public void onEnable() {
        isRunning = true;
        INSTANCE = this;

        // check minecraft version compatibility
        if (Protocol.getProtocol().isNewerThanOrEqual(Protocol.v1_17)) {
            getLogger().severe("Honey is currently not supported by Minecraft Version 1.17+! But don't panic, it will for sure come in the future! :-)");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        if (Protocol.getProtocol() == Protocol.UNKNOWN) {
            getLogger().severe("Honey is currently not supported by the Minecraft Version your server is using.");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        // setup configuration and check if it is misconfigured
        configuration = new DedicatedConfiguration();

        if (configuration.isMisConfigured()) {

            if (getServer().getPluginManager().isPluginEnabled(this)) {
                resetConfiguration();
                getServer().getPluginManager().disablePlugin(this);

                throw new RuntimeException("You have a misconfigured config.yml. The file is now deleted, you just have to restart the server.");
            }
            return;
        }

        // setup packet library
        packetLibrary = PacketLibrary.set(new PacketLibrarySettings(this, false, "honey_packet_handler", "honey_packet_decoder"));

        // setup services
        loggerService = new LoggerService();
        violationService = new ViolationService();
        notificationService = new NotificationService();
        confiscationService = new ConfiscationService();

        // setup sql handler
        sqlManager = new SQLManagerWrapper(new BeanRepositoryCollection());

        // setup trackers
        packetNettyHandleTracker = new PlayerNettyPacketHandleTracker(this);
        packetServerThreadHandleTimeTracker = new PlayerServerThreadHandleTracker(this);
        checkHandleTracker = new CheckHandleTracker(this);

        // setup checks
        registerChecks();
        ItemAnalysis.reload();

        // setup commands
        commandManager = new CommandManager(this);
        new HelpCommand(commandManager);
        new NotificationToggleCommand(commandManager);
        new ReloadCommand(commandManager);
        new PasteLogsCommand(commandManager);
        new TrackCommand(commandManager);
        new QuotientInfoCommand(commandManager);

        // setup metrics
        new Metrics(this, 19028);

        // update checker
        this.updateChecker = new UpdateChecker();
        this.usingOldVersion = updateChecker.isOldVersion();

        // setup bukkit listeners
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new DispenserListener(), this);

        // setup runnable getting the TPS and ping of the server and players
        new BukkitRunnable() {

            int index = 0;

            @Override
            public void run() {
                recentTPS = HoneyPlugin.get().getNotificationService().getTPS();

                if (index++ > 2) {
                    index = 0;

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        recentPingLookup.put(player.getUniqueId(), HoneyPlugin.get().getNotificationService().getPing(player));
                    }
                }
            }
        }.runTaskTimerAsynchronously(HoneyPlugin.get().getPlugin(), 0L, 20L);
    }

    @Override
    public void onDisable() {
        isRunning = false;

        if (PacketLibrary.get() != null) PacketLibrary.get().disable();
    }

    public <WP extends WrappedPacket<WP>> void registerChecks() {
        for (IListener check : ListenerRegistry.getChecks()) {

            if (check instanceof SyncPacketAdapter) {
                ListenerPriority priority = check.getPriority();

                if (check.getCheckType() == HoneyCheckType.PACKET && check instanceof IPacketListener) {
                    PacketLibrary.get().addListeners((SyncPacketAdapter<WP>) check, priority, ((IPacketListener)check).getPacketTypes());
                }
                if (check.getCheckType() == HoneyCheckType.DECODE) {
                    PacketLibrary.get().addDecodeListener((SyncPacketAdapter<?>) check, priority);
                }
                check.load();
                continue;
            }
            PacketLibrary.get().getEventManager().addListener(check);
        }
    }

    public void resetConfiguration() {
        File file = new File(getDataFolder(), "config.yml");

        if (file.exists()) {
            try {
                Files.delete(file.toPath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void reload() {
        configuration.reload();
        for (IListener check : ListenerRegistry.getChecks()) {
            check.load();
        }
        violationService.reload();
        loggerService.reload();
        confiscationService.reload();
        sqlManager.reload();
        ItemAnalysis.reload();
    }

    public static HoneyAPI get() {
        return INSTANCE;
    }

    public JavaPlugin getPlugin() {
        return this;
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

    @Override
    public SQLManagerWrapper getSQLManager() {
        return sqlManager;
    }

    public LoggerService getLoggerService() {
        return loggerService;
    }

    public StatisticHandleTracker<Double, Player> getNettyPacketHandleTracker() {
        return packetNettyHandleTracker;
    }

    public StatisticHandleTracker<Double, Player> getServerThreadHandleTracker() {
        return packetServerThreadHandleTimeTracker;
    }

    public MarkableHandleTracker<Double, String> getCheckHandleTracker() {
        return checkHandleTracker;
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

    public int getRecentTPS() {
        return recentTPS;
    }

    public int getRecentPing(Player player) {
        return recentPingLookup.computeIfAbsent(player.getUniqueId(), (d) -> notificationService.getPing(player));
    }

    public void removeRecentPingLookup(Player player) {
        recentPingLookup.remove(player.getUniqueId());
    }
}
