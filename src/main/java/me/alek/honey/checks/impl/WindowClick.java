package me.alek.honey.checks.impl;

import me.alek.honey.checks.PacketCheck;
import me.alek.honey.checks.helper.NBTHelper;
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

public class WindowClick extends AsyncPacketAdapter<WrappedPlayInWindowClick> implements PacketCheck {

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInWindowClick> packetContainer) {
        int slot = packetContainer.getPacket().getWindowSlot();
        if (slot > 103 || (slot < -1 && slot != -999)) {
            punish(player, packetContainer, "Illegal Slot");
            return;
        }
        ItemStack item = packetContainer.getPacket().getItem();
        if (NBTHelper.checkBukkitNBT(item)) {
            punishAndConfiscate(player, packetContainer, "Illegal NBT", item);
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.WINDOW_CLICK);
    }

    @Override
    public String getName() {
        return "Window Click";
    }
}
