package me.alek.honey.listeners.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.listeners.check.Check;
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

public class IllegalMovement<WP extends WrappedPlayInFlying<WP>> extends AsyncPacketAdapter<WP> implements Check {

    private static final ViolationService VIOLATION_SERVICE = HoneyPlugin.get().getViolationService();

    private final Map<UUID, AtomicInteger> chunkDiscoverMap = new ConcurrentHashMap<>();
    private final Map<UUID, Chunk> lastChunkMap = new ConcurrentHashMap<>();

    private final int maxChunksPerSecond;
    private final int maxDistanceIllegalMovement;

    public IllegalMovement() {
        maxChunksPerSecond = 20;
        maxDistanceIllegalMovement = 100;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getTypes() {
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
            VIOLATION_SERVICE.violate(player, packet.getPacket(), this, "Velocity");
        }
        Chunk chunk = packet.getPacket().getLocation().getChunk();
        if (lastChunkMap.get(player.getUniqueId()) == chunk) {
            return;
        }
        lastChunkMap.replace(player.getUniqueId(), chunk);

        if (chunkDiscoverMap.get(player.getUniqueId()).incrementAndGet() > maxChunksPerSecond) {
            packet.cancel();
            VIOLATION_SERVICE.violate(player, packet.getPacket(), this, "Chunk Overflow");
        }
    }

    @Override
    public String getName() {
        return "Illegal Movement";
    }

    @Override
    public void cleanup(Player player) {
        chunkDiscoverMap.remove(player.getUniqueId());
        lastChunkMap.remove(player.getUniqueId());
    }

    @Override
    public void resetVls(Player player) {
        chunkDiscoverMap.put(player.getUniqueId(), new AtomicInteger());
        lastChunkMap.remove(player.getUniqueId());
    }
}
