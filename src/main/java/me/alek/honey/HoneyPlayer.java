package me.alek.honey;

import io.netty.channel.Channel;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tickloop.TickloopHandler;
import me.alek.honey.tracker.temp.TempQueuedTracker;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.util.IDRegistry;
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
    public final int registryID = IDRegistry.PLAYERS.acquire();

    public boolean online = true;
    public boolean punished;
    public boolean updateHandlersLater;
    public HoneyPacketHandler packetHandler;
    public TempQueuedTracker timedQueuedTracker;
    public TempQueuedTracker serverThreadTimedQueuedTracker;
    public ViolationService.ViolationContext violationContext = new ViolationService.ViolationContext(this);
    public final TickloopHandler tickloopHandler = new TickloopHandler();
    public final CallbackAction<HoneyPlayer> leaveCallback = new CallbackAction<>();

    public HoneyPlayer(Player player) {
        this.player = player;

        uuid = player.getUniqueId();
        name = player.getName();
        channel = NMS.getChannel(name);
    }

    public void setPacketHandler(HoneyPacketHandler handler) {
        handler.setPlayer(this);
        this.packetHandler = handler;
    }

    public boolean isOnlineDirect() {
        return player.isOnline();
    }

    public boolean isOnlineWeak() {
        return online;
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
