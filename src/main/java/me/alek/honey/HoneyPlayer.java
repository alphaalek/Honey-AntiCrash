package me.alek.honey;

import io.netty.channel.Channel;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tickloop.postpone.PostponeCheck;
import me.alek.honey.tickloop.postpone.checkimpl.*;
import me.alek.honey.tracker.temp.TempQueuedTracker;
import me.alek.honey.util.Callback;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.ClientInfo;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class HoneyPlayer {

    private static final Map<UUID, HoneyPlayer> HONEY_PLAYER_LOOKUP = new HashMap<>();

    public static void register(HoneyPlayer honeyPlayer, Player player) {
        HONEY_PLAYER_LOOKUP.put(player.getUniqueId(), honeyPlayer);
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

    public final Channel channel;
    public final int registryID = IDRegistry.PLAYERS.acquire();

    public Player player;
    public UUID uuid;
    public String name;

    public boolean online = true;
    public boolean punished;
    public long packetIndex;
    public long postponedPackets;
    public long postponedPacketsForTick;
    public long postponedLastTick;
    public ClientInfo clientInfo;
    public HoneyPacketHandler packetHandler;
    public TempQueuedTracker timedQueuedTracker;
    public TempQueuedTracker serverThreadTimedQueuedTracker;
    public ViolationService.ViolationContext violationContext = new ViolationService.ViolationContext(this);
    public final Callback leaveCallback = new Callback();
    public Queue<PostponeCheck> postponedChecks = new LinkedList<>(
            new ArrayList<>(Arrays.asList(
                    new PostponeRateIgnoreTerminator(),
                    new PostponeRateCheck(),
                    new PostponeRateAccelCheck(),
                    new PostponeRateAccelQuotientCheck(),
                    new PostponeMaxPerXTickCheck()
            ))
    );

    public HoneyPlayer(Channel channel, HoneyPacketHandler handler) {
        this.channel = channel;
        this.packetHandler = handler;
    }

    public void initBukkitPlayer(Player player) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.name = player.getName();
    }

    public boolean isBukkitReady() {
        return this.player != null;
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
