package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInSetCreativeSlot;
import org.bukkit.entity.Player;

public class CreativeB implements PacketCheck<WrappedPlayInSetCreativeSlot> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInSetCreativeSlot packet, CheckContextContainer container) {
        int slot = packet.getSlot();
        if (slot > 103 || (slot < -1 && slot != -999)) {
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .punish()
                    .build();
        }
        return ResponseBuilder.SUCCESS;
    }
}
