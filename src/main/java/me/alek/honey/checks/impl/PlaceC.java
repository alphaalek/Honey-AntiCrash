package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.helper.NBTHelper;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInBlockPlace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlaceC implements PacketCheck<WrappedPlayInBlockPlace> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInBlockPlace packet, CheckContextContainer container) {
        ItemStack item = packet.getItem();
        ItemStack itemInHand = player.getItemInHand();

        if (NBTHelper.checkBukkitNBT(item) || NBTHelper.checkBukkitNBT(itemInHand)) {
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .punish()
                    .confiscate(item)
                    .build();
        }
        return ResponseBuilder.SUCCESS;
    }
}
