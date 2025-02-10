package me.alek.honey;

import io.netty.channel.Channel;
import me.alek.honey.compat.floodgate.Floodgate;
import me.alek.honey.compat.Paper;
import me.alek.honey.compat.floodgate.FloodgateValidator;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.impl.login.WrappedLoginInLoginStart;
import me.alek.honey.sql.SQLManager;
import me.alek.honey.tickloop.postpone.PostponeValidation;
import me.alek.honey.tickloop.postpone.validationimpl.*;
import me.alek.honey.tracker.temp.TempIntervalTracker;
import me.alek.honey.util.*;
import me.alek.honey.util.id.IntIdentifiable;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.validation.BaseValidationContainer;
import me.alek.honey.validation.ValidationContainerRegistry;
import me.alek.honey.validation.ViolationManagement;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Callable;

public class HoneyPlayer implements IntIdentifiable {

    public enum Phase {
        INITIALIZING, LOGGING_IN, ONLINE, OFFLINE
    }

    private static final Map<UUID, HoneyPlayer> HONEY_PLAYER_LOOKUP = new HashMap<>();

    public static void register(HoneyPlayer honeyPlayer, Player player) {
        HONEY_PLAYER_LOOKUP.put(player.getUniqueId(), honeyPlayer);
    }

    public static HoneyPlayer getPlayer(UUID uuid) {
        return HONEY_PLAYER_LOOKUP.get(uuid);
    }

    public static Collection<HoneyPlayer> getAllPlayers() {
        return HONEY_PLAYER_LOOKUP.values();
    }

    private static final String DISCONNECTED = "Disconnected";

    public final Channel channel;
    public final int registryID = IDRegistry.PLAYER.acquire();
    public final long tickInitialized = HoneyPlugin.get().getExecutionQueueProxy().getCurrentExecutionTick();

    public Player bukkit;
    public UUID uuid;
    public String name = HoneyPacketHandler.UNKNOWN;
    public boolean floodgate;
    public boolean pinging;
    public Phase phase = Phase.INITIALIZING;
    public boolean punished;
    public long postponedPackets;
    public long postponedPacketsForTick;
    public long postponedLastTick;
    public ClientInfo clientInfo;
    public HoneyPacketHandler packetHandler;
    public TempIntervalTracker nettyThreadTimedQueuedTracker;
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
        this.channel = channel;
        this.packetHandler = handler;

        if (this.channel.localAddress() != null) {
            this.name = ((InetSocketAddress)this.channel.remoteAddress()).getAddress().getHostAddress();
        }
    }

    public void setupHello(WrappedLoginInLoginStart hello) {
        this.uuid = hello.getUUID();

        if (FloodgateValidator.HAS_FLOODGATE) {
            String floodgateUserName = Floodgate.getFloodgateUsername(hello);
            if (floodgateUserName != null) {
                this.name = floodgateUserName;
                this.floodgate = true;
                return;
            }
        }
        this.name = hello.getUsername();
    }

    public void onVersionUpdate(int protocol) {
        if (protocol == ClientInfo.PINGING_PROTOCOL_VERSION) {
            this.clientInfo = ClientInfo.NON_EXISTENT;
            this.pinging = true;
        } else {
            this.clientInfo = new ClientInfo.Default(protocol);
        }
    }

    public synchronized void remove() {
        HONEY_PLAYER_LOOKUP.remove(this.uuid);
        IDRegistry.PLAYER.remove(this.registryID);

        this.phase = Phase.OFFLINE;

        for (BaseValidationContainer validationContainer : ValidationContainerRegistry.getValidations()) {
            validationContainer.remove(this);
        }

        HoneyPlugin.get().getLogger().removePacketData(this);
        HoneyPlugin.get().getNotificationService().unsubscribe(bukkit);

        HoneyPlugin.get().getNettyPacketTracker().remove(this);
        HoneyPlugin.get().getExecutionQueueProxy().remove(this);

        HoneyPlugin.get().getMonitorManager().removeFromMonitors(this);

        this.leaveCallback.call(this);
    }

    public void initBukkitPlayer(Player player) {
        this.bukkit = player;
        this.phase = Phase.ONLINE;

        for (BaseValidationContainer validationContainer : ValidationContainerRegistry.getValidations()) {
            validationContainer.setup(this);
        }
        HoneyPlugin.get().getNotificationService().setup(this);

        if (this.bukkit.isOp() && HoneyPlugin.get().isUsingOldVersion()) {
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

    public String getFailedPacketCause(Throwable ex) {
        if (ex.getCause() == null || ex.getCause().getMessage() == null) {
            return ex.getLocalizedMessage();
        }
        StringBuilder msg = new StringBuilder();
        Throwable exc = ex.getCause();
        while (exc != null && exc.getMessage() != null) {
            msg.append(": ").append(exc.getMessage());
            exc = exc.getCause();
        }
        return msg.substring(2);
    }

    public void failPacket(Throwable ex, PacketContainer<?> packet) {
        if (packet != null) {
            packet.releaseIfAutoReleasable();
        }

        this.phase = Phase.OFFLINE;
        if (this.bukkit != null) {
            if (ex.getMessage() != null) {
                kick(getFailedPacketCause(ex));
            } else {
                kick(DISCONNECTED);
            }
        } else {
            ChannelCloseInspection.close(this.channel);
        }
    }

    public void kick() {
        kick(DISCONNECTED);
    }

    public void kick(String reason) {
        kick(this.bukkit, reason);
    }

    @SuppressWarnings("deprecation")
    public static void kick(Player player, String reason) {
        Bukkit.getScheduler().runTask(HoneyPlugin.get().getPlugin(), () -> {
            if (Paper.IS_PAPER && NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17)) {
                player.kick(Component.text(reason));
            } else {
                player.kickPlayer(reason);
            }
        });
    }

    public boolean isStreamActive() {
        return !this.punished
                && this.phase != Phase.OFFLINE
                && this.channel.isOpen();
    }

    public boolean linkedBukkit() {
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
                    && linkedBukkit()
                    && this.bukkit.hasPermission(ConfigurationOptions.General.BYPASS_PERMISSION);
        });
    }

    public boolean bypassEverything() {
        return this.bypassEverything.get(() -> {
            return ConfigurationOptions.General.BYPASS_PERMISSION_ENABLED
                    && linkedBukkit()
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
        return this.name;
    }

    @Override
    public Integer getID() {
        return this.registryID;
    }
}
