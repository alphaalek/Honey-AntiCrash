package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.IntervalCheck;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public class PositionB<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, IntervalCheck {

    @Override
    public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
        Chunk chunk = container.getChunk(0);
        if (chunk == null) {
            return ResponseBuilder.SUCCESS;
        }
        if (player.getWorld().isChunkLoaded(chunk) && chunk.isLoaded()) {
            return ResponseBuilder.SUCCESS;
        }
        return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
    }

    @Override
    public ConfigurationOptions.IntervalSetting getSetting() {
        return ConfigurationOptions.Velocity.NewUnloadedChunks.get();
    }
}
