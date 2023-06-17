package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.helper.NBTHelper;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInSetCreativeSlot;
import org.bukkit.entity.Player;

public class CreativeC implements PacketCheck<WrappedPlayInSetCreativeSlot> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInSetCreativeSlot packet, CheckContextContainer container) {
        if (NBTHelper.checkBukkitNBT(packet.getItem())) {
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .punish()
                    .confiscate(packet.getItem())
                    .build();
        }
        return ResponseBuilder.SUCCESS;
    }
}
