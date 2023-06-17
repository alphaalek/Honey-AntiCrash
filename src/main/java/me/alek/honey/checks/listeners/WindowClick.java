package me.alek.honey.checks.listeners;

import me.alek.honey.checks.*;
import me.alek.honey.checks.helper.NBTHelper;
import me.alek.honey.checks.impl.WindowA;
import me.alek.honey.checks.impl.WindowB;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInWindowClick;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class WindowClick extends AbstractPacketListener<WrappedPlayInWindowClick> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInWindowClick> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInWindowClick> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedPlayInWindowClick> getVisitor() {
        CheckVisitor<WrappedPlayInWindowClick> visitor = new CheckVisitor<>();
        visitor.visit(new WindowA());
        visitor.visit(new WindowB());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.WINDOW_CLICK);
    }
}
