package me.alek.honey.checks.impl.velocity;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.wrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.services.ViolationService;
import me.alek.honey.utility.Locations;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class IllegalVelocity<WP extends WrappedPlayInFlying<WP>> extends AsyncPacketAdapter<WP> implements Check {

    private static final ViolationService VIOLATION_SERVICE = HoneyPlugin.get().getViolationService();

    private final Map<UUID, AtomicInteger> unloadedChunkDiscoverMap = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> chunkDiscoverMap = new ConcurrentHashMap<>();
    private final Map<UUID, Chunk> lastChunkMap = new ConcurrentHashMap<>();

    private final int maxChunksPerInterval;
    private final int maxUnloadedChunksPerInterval;
    private final int maxDistanceIllegalMovement;

    public IllegalVelocity() {
        maxChunksPerInterval = 20;
        maxUnloadedChunksPerInterval = 15;
        maxDistanceIllegalMovement = 20;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK
        );
    }

    @Override
    public void onPacketReceive(Player player, PacketContainer<WP> packet) {
        if (Locations.compare3DLocation(packet.getPacket().getLocation(), player.getLocation())) {
            return;
        }
        final int distance = Locations.calculate2DDistance(packet.getPacket().getLocation(), player.getLocation());
        if (distance > maxDistanceIllegalMovement) {
            packet.cancel();
            VIOLATION_SERVICE.violate(player, packet.getPacket(), this, "Illegal Movement");
            return;
        }
        Chunk chunk = packet.getPacket().getWorld().getChunkAt(packet.getPacket().getLocation());
        if (lastChunkMap.get(player.getUniqueId()) == chunk) {
            return;
        }
        lastChunkMap.put(player.getUniqueId(), chunk);

        if (chunkDiscoverMap.get(player.getUniqueId()).incrementAndGet() > maxChunksPerInterval) {
            packet.cancel();
            VIOLATION_SERVICE.violate(player, packet.getPacket(), this, "Chunk Overflow");
        }
        if (!chunk.isLoaded() || !packet.getPacket().getWorld().isChunkLoaded(chunk)) {
            if (unloadedChunkDiscoverMap.get(player.getUniqueId()).incrementAndGet() > maxUnloadedChunksPerInterval) {
                packet.cancel();
                VIOLATION_SERVICE.violate(player, packet.getPacket(), this, "Unloaded Chunk Overflow");
            }
        }
    }

    @Override
    public String getName() {
        return "Velocity";
    }

    @Override
    public void setup(Player player) {
        resetVls(player);
    }

    @Override
    public void cleanup(Player player) {
        final UUID uuid = player.getUniqueId();
        chunkDiscoverMap.remove(uuid);
        unloadedChunkDiscoverMap.remove(uuid);
        lastChunkMap.remove(player.getUniqueId());
    }

    @Override
    public void resetVls(Player player) {
        final UUID uuid = player.getUniqueId();
        chunkDiscoverMap.put(uuid, new AtomicInteger());
        unloadedChunkDiscoverMap.put(uuid, new AtomicInteger());
        lastChunkMap.remove(uuid);
    }
}
