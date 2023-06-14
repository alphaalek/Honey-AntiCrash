package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.checks.helper.NBTHelper;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInWindowClick;
import me.alek.honey.services.ViolationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class WindowClick extends AsyncPacketAdapter<WrappedPlayInWindowClick> implements Check {

    private final ViolationService VIOLATION_SERVICE = HoneyPlugin.get().getViolationService();

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInWindowClick> packetContainer) {
        int slot = packetContainer.getPacket().getWindowSlot();
        if (slot > 103 || (slot < -1 && slot != -999)) {
            VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this, "Illegal Slot");
            packetContainer.cancel();
            return;
        }
        Object item = packetContainer.getPacket().getItem();
        if (NBTHelper.checkNMSNBT(item)) {
            VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this, "Illegal NBT");
            packetContainer.cancel();
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
