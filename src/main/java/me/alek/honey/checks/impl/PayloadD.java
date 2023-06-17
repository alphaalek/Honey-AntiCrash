package me.alek.honey.checks.impl;

import io.netty.buffer.ByteBuf;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.helper.ItemHelper;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PayloadD implements PacketCheck<WrappedPlayInCustomPayload> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
        String channel = packet.getChannel();
        ItemStack itemInHand = player.getItemInHand();

        if (!channel.contains("BSign") && !channel.contains("BEdit") && !channel.contains("BOpen")) {
            if (!channel.isEmpty()) {
                return ResponseBuilder.SUCCESS;
            }
        }
        else {
            if (ItemHelper.isBook(itemInHand)) {
                return ResponseBuilder.SUCCESS;
            }
        }
        ByteBuf serializer = container.getSerializer(0);
        if (serializer != null) {
            serializer.release();
        }

        ItemStack bukkitItem = container.getItem(0);
        return ResponseBuilder
                .inst()
                .cancel()
                .punish()
                .confiscate(bukkitItem)
                .build();
    }
}
