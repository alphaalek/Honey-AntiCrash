package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.IntervalCheck;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class PositionA<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, IntervalCheck {

    @Override
    public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
        Map<?, ?> lastChunkMap = container.getMap(0);
        if (lastChunkMap == null) {
            return ResponseBuilder.SUCCESS.builder().skip(1).build();
        }
        Chunk chunk = packet.getWorld().getChunkAt(packet.getLocation());
        if (lastChunkMap.get(player.getUniqueId()) == chunk) {
            return ResponseBuilder.SUCCESS.builder().skip(1).build();
        }
        container.putChunk(chunk);
        try {
            ((Map<UUID, Chunk>)lastChunkMap).put(player.getUniqueId(), chunk);
        } catch (Exception ex) {
            return ResponseBuilder.SUCCESS.builder().skip(1).build();
        }
        return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
    }

    @Override
    public ConfigurationOptions.IntervalSetting getSetting() {
        return ConfigurationOptions.Velocity.NewChunks.get();
    }

}
