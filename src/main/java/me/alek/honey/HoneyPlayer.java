package me.alek.honey;

import io.netty.channel.Channel;
import me.alek.honey.utility.CallbackAction;
import me.alek.honey.netty.packet.bundle.PacketBundleSaver;
import me.alek.honey.netty.utility.NMS;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tracker.temp.impl.TempTimedQueuedTracker;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HoneyPlayer {

    private static final Map<UUID, HoneyPlayer> HONEY_PLAYER_LOOKUP = new HashMap<>();

    public static HoneyPlayer registerPlayer(Player player) {
        HoneyPlayer honeyPlayer = new HoneyPlayer(player);
        HONEY_PLAYER_LOOKUP.put(player.getUniqueId(), honeyPlayer);
        return honeyPlayer;
    }

    public static HoneyPlayer getPlayer(UUID uuid) {
        return HONEY_PLAYER_LOOKUP.get(uuid);
    }

    public static void removePlayer(UUID uuid) {
        HONEY_PLAYER_LOOKUP.remove(uuid);
    }

    public static Collection<HoneyPlayer> getAllPlayers() {
        return HONEY_PLAYER_LOOKUP.values();
    }

    public final Player player;
    public final UUID uuid;
    public final String name;
    public final Channel channel;

    public boolean updateHandlersLater;
    public boolean removedPacketHandlerFromPipeline;
    public TempTimedQueuedTracker timedQueuedTracker;
    public TempTimedQueuedTracker serverThreadTimedQueuedTracker;
    public final CallbackAction<HoneyPlayer> leaveCallback = new CallbackAction<>();
    public PacketBundleSaver packetBundleSaver = new PacketBundleSaver();
    public ViolationService.ViolationContext violationContext;
    public boolean punished;
    public int registryID;

    public HoneyPlayer(Player player) {
        this.player = player;

        uuid = player.getUniqueId();
        name = player.getName();
        channel = NMS.getChannelIfSet(name);
        violationContext = new ViolationService.ViolationContext(this);

        registryID = IDRegistry.PLAYERS.acquireID();
    }

    public boolean isOnline() {
        return player.isOnline();
    }

    public World getWorld() {
        return getLocation().getWorld();
    }

    public Location getLocation() {
        return player.getLocation();
    }

    public void sendMessage(String message) {
        player.sendMessage(message);
    }
}
