package me.alek.honey.checks.impl;

import io.netty.buffer.ByteBuf;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PayloadB implements PacketCheck<WrappedPlayInCustomPayload> {
    @Override
    public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
        ByteBuf serializer = container.getSerializer(0);
        if (serializer == null) {
            return ResponseBuilder.SUCCESS;
        }
        final int refCnt = serializer.refCnt();
        final int capacity = serializer.capacity();

        if (       capacity > ConfigurationOptions.CustomPayload.MAX_BYTEBUF_CAPACITY
                || capacity < ConfigurationOptions.CustomPayload.MIN_BYTEBUF_CAPACITY
                || refCnt > ConfigurationOptions.CustomPayload.MAX_BYTEBUF_REFCNT
                || refCnt < ConfigurationOptions.CustomPayload.MIN_BYTEBUF_REFCNT
        ) {
            serializer.release();
            ItemStack bukkitItem = container.getItem(0);

            return ResponseBuilder
                    .inst()
                    .cancel()
                    .punish()
                    .confiscate(bukkitItem)
                    .build();
        }
        return ResponseBuilder.SUCCESS;
    }
}
