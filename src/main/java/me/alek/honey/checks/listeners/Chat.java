package me.alek.honey.checks.listeners;

import me.alek.honey.checks.*;
import me.alek.honey.checks.impl.ChatA;
import me.alek.honey.checks.impl.CommandsA;
import me.alek.honey.checks.types.Check;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInChat;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class Chat extends AbstractPacketListener<WrappedPlayInChat> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInChat> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInChat> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedPlayInChat> getVisitor() {
        CheckVisitor<WrappedPlayInChat> visitor = new CheckVisitor<>();
        visitor.visit(new ChatA());
        visitor.visit(new CommandsA());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CHAT);
    }

}
