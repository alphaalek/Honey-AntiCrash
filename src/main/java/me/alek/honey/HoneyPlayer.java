package me.alek.honey;

import io.netty.channel.Channel;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.sql.SQLManager;
import me.alek.honey.tickloop.postpone.PostponeValidation;
import me.alek.honey.tickloop.postpone.validationimpl.*;
import me.alek.honey.tracker.temp.TempIntervalTracker;
import me.alek.honey.util.*;
import me.alek.honey.validation.BaseValidationContainer;
import me.alek.honey.validation.ValidationContainerRegistry;
import me.alek.honey.validation.ViolationManagement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class HoneyPlayer implements Identifiable {

    private static final Map<UUID, HoneyPlayer> HONEY_PLAYER_LOOKUP = new HashMap<>();
    private static final Array<HoneyPlayer> PLAYERS = new Array<>();

    public static void register(HoneyPlayer honeyPlayer, Player player) {
        HONEY_PLAYER_LOOKUP.put(player.getUniqueId(), honeyPlayer);
    }

    public static HoneyPlayer getPlayer(UUID uuid) {
        return HONEY_PLAYER_LOOKUP.get(uuid);
    }

    public static Collection<HoneyPlayer> getAllPlayers() {
        return HONEY_PLAYER_LOOKUP.values();
    }

    public final Channel channel;
    public final int registryID = IDRegistry.PLAYERS.acquire();

    public Player bukkit;
    public UUID uuid;
    public String name;

    public boolean online = false;
    public boolean punished;
    public long postponedPackets;
    public long postponedPacketsForTick;
    public long postponedLastTick;
    public ClientInfo clientInfo;
    public HoneyPacketHandler packetHandler;
    public TempIntervalTracker timedQueuedTracker;
    public TempIntervalTracker serverThreadTimedQueuedTracker;
    public ViolationManagement.ViolationContext violationContext = new ViolationManagement.ViolationContext(this);
    public final Callback leaveCallback = new Callback();
    private final TickCache<Boolean> bypassEverything = new TickCache<>();
    private final TickCache<Boolean> bypassServerThreadTracking = new TickCache<>();

    public EnhancedInitialIterator<PostponeValidation> postponedChecks = new EnhancedInitialIterator<>(
            new LinkedList<>(
                new ArrayList<>(Arrays.asList(
                        new PostponeRateIgnoreTerminator(),
                        new PostponeRateCheck(),
                        new PostponeRateAccelCheck(),
                        new PostponeRateAccelQuotientCheck(),
                        new PostponeMaxPerXTickCheck()
                ))
            ),
            i -> new LazyInit<>(() -> ((LinkedList<PostponeValidation>)i).peek())
    );

    public HoneyPlayer(Channel channel, HoneyPacketHandler handler) {
        PLAYERS.set(registryID, this);
        this.channel = channel;
        this.packetHandler = handler;
    }

    public void remove() {
        online = false;

        for (BaseValidationContainer validationContainer : ValidationContainerRegistry.getValidations()) {
            validationContainer.cleanup(this);
        }

        HoneyPlugin.get().getLogger().removePacketData(this);
        HoneyPlugin.get().getNotificationService().unsubscribe(bukkit);

        HoneyPlugin.get().getNettyPacketTracker().remove(this);
        HoneyPlugin.get().getExecutionQueueProxy().remove(this);

        HoneyPlugin.get().getMonitorManager().removeFromMonitors(this);

        leaveCallback.call(this);
        HONEY_PLAYER_LOOKUP.remove(uuid);
        PLAYERS.remove(registryID);
        IDRegistry.PLAYERS.remove(registryID);
    }

    public void initBukkitPlayer(Player player) {
        this.bukkit = player;
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.online = true;

        for (BaseValidationContainer validationContainer : ValidationContainerRegistry.getValidations()) {
            validationContainer.setup(this);
        }
        HoneyPlugin.get().getNotificationService().setup(this);

        if (bukkit.isOp() && HoneyPlugin.get().isUsingOldVersion()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.sendMessage(Notifications.PREFIX + " §7Found an update: v" + HoneyPlugin.get().getLatestVersion() + ". Download:");
                    player.sendMessage("§ahttps://builtbybit.com/resources/honey-anti-crash-anti-packet-exploit.40905/");
                }
            }.runTaskLater(HoneyPlugin.get().getPlugin(), 40L);
        }

        SQLManager sqlHandler = HoneyPlugin.get().getSQLManager();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (sqlHandler.isEnabled()) {
                    try {
                        sqlHandler.preparePlayer(HoneyPlayer.this);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
    }

    public boolean isStreamActive() {
        return !punished && (!isBukkitReady() || online) && channel.isOpen();
    }

    public boolean isBukkitReady() {
        return this.bukkit != null;
    }

    public boolean isInLiquid() {
        Material material = bukkit.getEyeLocation().getBlock().getType();
        return material.name().contains("WATER")
                || material.name().contains("LAVA");
    }

    public boolean bypassServerThreadTracking() {
        return bypassServerThreadTracking.get(() -> {
            return ConfigurationOptions.General.BYPASS_PERMISSION_SERVER_THREAD_ENABLED
                    && isBukkitReady()
                    && bukkit.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION);
        });
    }

    public boolean bypassEverything() {
        return bypassEverything.get(() -> {
            return ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED
                    && isBukkitReady()
                    && bukkit.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION);
        });
    }

    public boolean isOnlineBukkit() {
        return bukkit.isOnline();
    }

    public World getWorld() {
        return getLocation().getWorld();
    }

    public Location getLocation() {
        return bukkit.getLocation();
    }

    public void sendMessage(String message) {
        bukkit.sendMessage(message);
    }

    @Override
    public String toString() {
        return packetHandler.getPlayerName();
    }

    @Override
    public int getID() {
        return registryID;
    }
}
