package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInHeldItemSlot;
import org.bukkit.entity.Player;

public class HeldItemA implements PacketCheck<WrappedPlayInHeldItemSlot> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInHeldItemSlot packet, CheckContextContainer container) {
        if (packet.getSlot() >= 36 || packet.getSlot() < 0) {
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .punish()
                    .build();
        }
        return ResponseBuilder.SUCCESS;
    }
}
