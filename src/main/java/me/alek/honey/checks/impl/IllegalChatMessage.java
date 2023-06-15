package me.alek.honey.checks.impl;

import me.alek.honey.checks.PacketCheck;
import me.alek.honey.checks.helper.NBTHelper;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInChat;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class IllegalChatMessage extends AsyncPacketAdapter<WrappedPlayInChat> implements PacketCheck {

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInChat> packetContainer) {
        String message = packetContainer.getPacket().getMessage();
        if (message.startsWith("/")) {
            return;
        }
        if (NBTHelper.checkBadCharacter(message)) {
            punish(player, packetContainer, "Illegal Message");
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CHAT);
    }

    @Override
    public String getName() {
        return "Chat";
    }
}
