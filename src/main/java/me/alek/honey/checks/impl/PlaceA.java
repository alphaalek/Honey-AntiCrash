package me.alek.honey.checks.impl;

import me.alek.honey.checks.types.IntervalCheck;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.helper.wrappers.models.WrappedBlockPosition;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInBlockPlace;
import org.bukkit.entity.Player;

public class PlaceA implements IntervalCheck, PacketCheck<WrappedPlayInBlockPlace> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInBlockPlace packet, CheckContextContainer container) {
        WrappedBlockPosition blockPosition = packet.getBlockPosition();
        if (!blockPosition.equals(WrappedBlockPosition.DEFAULT)) {

            double distance = blockPosition.distance(player.getLocation());
            if (distance > ConfigurationOptions.BlockPlacement.MAX_BLOCK_TO_PLAYER_DISTANCE) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .flag()
                        .build();
            }
        }
        return ResponseBuilder.SUCCESS;
    }

    @Override
    public ConfigurationOptions.IntervalSetting getSetting() {
        return ConfigurationOptions.BlockPlacement.get();
    }
}
