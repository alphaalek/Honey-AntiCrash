package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.helper.NBTHelper;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInWindowClick;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WindowB implements PacketCheck<WrappedPlayInWindowClick> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInWindowClick packet, CheckContextContainer container) {
        ItemStack item = packet.getItem();
        if (NBTHelper.checkBukkitNBT(item)) {
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
