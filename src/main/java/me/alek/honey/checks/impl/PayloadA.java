package me.alek.honey.checks.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import org.bukkit.entity.Player;

public class PayloadA implements PacketCheck<WrappedPlayInCustomPayload> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInCustomPayload packet, CheckContextContainer container) {
        ByteBuf serializer = container.getSerializer(0);
        if (serializer == null || serializer instanceof EmptyByteBuf) {
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .punish()
                    .build();
        }
        return ResponseBuilder.SUCCESS;
    }
}
