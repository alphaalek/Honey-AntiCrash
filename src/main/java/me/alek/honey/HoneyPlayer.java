package me.alek.honey;

import io.netty.channel.Channel;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.impl.login.WrappedLoginOutDisconnect;
import me.alek.honey.sql.SQLManager;
import me.alek.honey.tickloop.postpone.PostponeValidation;
import me.alek.honey.tickloop.postpone.validationimpl.*;
import me.alek.honey.tracker.temp.TempIntervalTracker;
import me.alek.honey.util.*;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.validation.BaseValidationContainer;
import me.alek.honey.validation.ValidationContainerRegistry;
import me.alek.honey.validation.ViolationManagement;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.Callable;

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

    private static final Component DISCONNECTED = Component.text("Disconnected");

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

    public EnhancedInitialIterator<PostponeValidation, LinkedList<PostponeValidation>> postponedChecks = new EnhancedInitialIterator<>(
            new LinkedList<>(
                    Arrays.asList(
                            new PostponeRateIgnoreTerminator(),
                            new PostponeRateCheck(),
                            new PostponeRateAccelCheck(),
                            new PostponeRateAccelQuotientCheck(),
                            new PostponeMaxPerXTickCheck()
                    )
            ),
            Queue::peek
    );

    public HoneyPlayer(Channel channel, HoneyPacketHandler handler) {
        PLAYERS.set(this.registryID, this);
        this.channel = channel;
        this.packetHandler = handler;
    }

    public void onVersionUpdate(int protocol) {
        this.clientInfo = new ClientInfo(protocol);
    }

    public void remove() {
        this.online = false;

        for (BaseValidationContainer validationContainer : ValidationContainerRegistry.getValidations()) {
            validationContainer.cleanup(this);
        }

        HoneyPlugin.get().getLogger().removePacketData(this);
        HoneyPlugin.get().getNotificationService().unsubscribe(bukkit);

        HoneyPlugin.get().getNettyPacketTracker().remove(this);
        HoneyPlugin.get().getExecutionQueueProxy().remove(this);

        HoneyPlugin.get().getMonitorManager().removeFromMonitors(this);

        this.leaveCallback.call(this);
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

    public void executeExternally(Runnable runnable, Callback exCallback) {
        try {
            runnable.run();
        } catch (Exception ex) {
            if (exCallback != null) {
                exCallback.call(ex);
            }
        }
    }

    public <T> T executeExternally(Callable<T> callable, Callback exCallback) {
        try {
            return callable.call();
        } catch (Exception ex) {
            if (exCallback != null) {
                exCallback.call(ex);
            }
            return null;
        }
    }

    public void failPacket(Throwable ex, PacketContainer<?> packet) {
        if (packet != null) {
            packet.releaseIfAutoReleasable();
        }

        this.online = false;
        HoneyPlugin.get().sync(() -> {
            if (this.bukkit != null) {
                if (ex != null && ex.getMessage() != null) {
                    kick(Component.text(ex.getMessage()));
                } else {
                    kick(DISCONNECTED);
                }
            }
        });
    }

    public void kick() {
        kick(DISCONNECTED);
    }

    public void kick(Component reason) {
        this.online = false;
        WrappedLoginOutDisconnect packet = new WrappedLoginOutDisconnect(this.clientInfo.getProtocol(), reason);
        packet.sendSilently(this.channel);
    }

    public boolean isStreamActive() {
        return !this.punished && (!isBukkitReady() || this.online) && this.channel.isOpen();
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
        return this.bypassServerThreadTracking.get(() -> {
            return ConfigurationOptions.General.BYPASS_PERMISSION_SERVER_THREAD_ENABLED
                    && isBukkitReady()
                    && this.bukkit.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION);
        });
    }

    public boolean bypassEverything() {
        return this.bypassEverything.get(() -> {
            return ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED
                    && isBukkitReady()
                    && this.bukkit.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION);
        });
    }

    public boolean isOnlineBukkit() {
        return this.bukkit.isOnline();
    }

    public World getWorld() {
        return getLocation().getWorld();
    }

    public Location getLocation() {
        return this.bukkit.getLocation();
    }

    public void sendMessage(String message) {
        this.bukkit.sendMessage(message);
    }

    public ItemStack getItemInMainHand() {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            return this.bukkit.getInventory().getItemInHand();
        } else {
            return this.bukkit.getInventory().getItemInMainHand();
        }
    }

    public void setItemInMainHand(ItemStack item) {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            this.bukkit.getInventory().setItemInHand(item);
        } else {
            this.bukkit.getInventory().setItemInMainHand(item);
        }
    }

    @Override
    public String toString() {
        return this.packetHandler.getPlayerName();
    }

    @Override
    public int getID() {
        return this.registryID;
    }
}
