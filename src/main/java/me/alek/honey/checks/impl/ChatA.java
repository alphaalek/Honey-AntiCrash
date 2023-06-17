package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.helper.NBTHelper;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInChat;
import org.bukkit.entity.Player;

public class ChatA implements PacketCheck<WrappedPlayInChat> {

    @Override
    public ResponseContainer accept(Player player, WrappedPlayInChat packet, CheckContextContainer container) {
        String message = packet.getMessage();
        if (message.startsWith("/")) {
            return ResponseBuilder.SUCCESS;
        }
        if (NBTHelper.checkBadCharacter(message)) {
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .punish()
                    .build();
        }
        return ResponseBuilder.SUCCESS;
    }
}
