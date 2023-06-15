package me.alek.honey.checks.impl;

import me.alek.honey.checks.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.utility.Locations;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class IllegalVelocity<WP extends WrappedPlayInFlying<WP>> extends AsyncPacketAdapter<WP> implements PacketCheck {

    private final Map<UUID, Chunk> lastChunkMap = new ConcurrentHashMap<>();

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Arrays.asList(
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK
        );
    }

    @Override
    public void onPacketReceive(Player player, PacketContainer<WP> packetContainer) {
        final WrappedPlayInFlying<WP> packet = packetContainer.getPacket();
        if (Locations.compare3DLocation(packet.getLocation(), player.getLocation())) {
            return;
        }
        final int distance = Locations.calculate2DDistance(packet.getLocation(), player.getLocation());
        if (distance > ConfigurationOptions.Velocity.Movement.MAX_2D_DISTANCE) {
            flag(player, packetContainer, this, "Illegal Movement", ConfigurationOptions.Velocity.Movement.get());
            return;
        }
        Chunk chunk = packet.getWorld().getChunkAt(packet.getLocation());
        if (lastChunkMap.get(player.getUniqueId()) == chunk) {
            return;
        }
        lastChunkMap.put(player.getUniqueId(), chunk);

        flagButAccept(player, packetContainer, this, "Chunk Overflow", ConfigurationOptions.Velocity.NewChunks.get());
        if (!chunk.isLoaded() && !packet.getWorld().isChunkLoaded(chunk)) {
            flagButAccept(player, packetContainer, this, "Chunk Overflow", ConfigurationOptions.Velocity.NewUnloadedChunks.get());
        }
    }

    @Override
    public String getName() {
        return "Velocity";
    }

    @Override
    public void cleanup(Player player) {
        lastChunkMap.remove(player.getUniqueId());
    }

    @Override
    public void onResetVls(Player player) {
        lastChunkMap.remove(player.getUniqueId());
    }
}
