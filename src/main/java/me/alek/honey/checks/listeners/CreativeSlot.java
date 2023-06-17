package me.alek.honey.checks.listeners;

import me.alek.honey.checks.*;
import me.alek.honey.checks.impl.CreativeA;
import me.alek.honey.checks.impl.CreativeB;
import me.alek.honey.checks.impl.CreativeC;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInSetCreativeSlot;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CreativeSlot extends AbstractPacketListener<WrappedPlayInSetCreativeSlot> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInSetCreativeSlot> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedPlayInSetCreativeSlot> getVisitor() {
        CheckVisitor<WrappedPlayInSetCreativeSlot> visitor = new CheckVisitor<>();
        visitor.visit(new CreativeA());
        visitor.visit(new CreativeB());
        visitor.visit(new CreativeC());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.SET_CREATIVE_SLOT);
    }
}
