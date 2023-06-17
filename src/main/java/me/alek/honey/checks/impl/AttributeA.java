package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.ContainerCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.entity.Player;

public class AttributeA implements ContainerCheck {

    @Override
    public ResponseContainer accept(Player player, PacketContainer packet, CheckContextContainer container) {
        for (double value : packet.getDoubles().readValues()) {

            if (NumberConversions.isInvalid(value)) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .build();
            }
        }
        for (float value : packet.getFloats().readValues()) {

            if (NumberConversions.isInvalid(value)) {
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
