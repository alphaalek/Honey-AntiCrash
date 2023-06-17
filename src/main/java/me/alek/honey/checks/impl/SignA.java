package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInUpdateSign;
import org.bukkit.entity.Player;

public class SignA implements PacketCheck<WrappedPlayInUpdateSign> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInUpdateSign packet, CheckContextContainer container) {
        for (String line : packet.getLines()) {
            if (line.length() > 49) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .build();
            }
        }
        return ResponseBuilder.SUCCESS;
    }
}
