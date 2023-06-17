package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.IntervalCheck;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInFlying;
import me.alek.honey.utility.Locations;
import org.bukkit.entity.Player;

public class PositionC<WP extends WrappedPlayInFlying<WP>> implements PacketCheck<WP>, IntervalCheck {

    @Override
    public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
        if (packet.getLocation().getChunk() == null) {
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .punish()
                    .build();
        }
        final int distance = Locations.calculate2DDistance(packet.getLocation(), player.getLocation());
        if (distance > ConfigurationOptions.Velocity.Movement.MAX_2D_DISTANCE) {
            player.teleport(player.getLocation());
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .flag()
                    .build();
        }
        return ResponseBuilder.SUCCESS;
    }

    @Override
    public ConfigurationOptions.IntervalSetting getSetting() {
        return ConfigurationOptions.Velocity.Movement.get();
    }
}
