package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.checks.helper.ItemHelper;
import me.alek.honey.checks.helper.NBTHelper;
import me.alek.honey.checks.helper.wrappers.models.WrappedBlockPosition;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInBlockPlace;
import me.alek.honey.services.ViolationService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Supplier;

public class BlockPlacement extends AsyncPacketAdapter<WrappedPlayInBlockPlace> implements Check {

    private final ViolationService VIOLATION_SERVICE = HoneyPlugin.get().getViolationService();

    @Override
    public void onPacketReceive(Player player, PacketContainer<WrappedPlayInBlockPlace> packetContainer) {
        final ItemStack itemInHand = player.getItemInHand();
        if (itemInHand.getType() == Material.AIR) {
            return;
        }

        WrappedBlockPosition blockPosition = packetContainer.getPacket().getBlockPosition();
        if (!blockPosition.equals(WrappedBlockPosition.DEFAULT)) {

            double distance = blockPosition.distance(player.getLocation());
            if (distance > ConfigurationOptions.BlockPlacement.MAX_BLOCK_TO_PLAYER_DISTANCE) {
                VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this, "Illegal Placement");
                packetContainer.cancel();
                return;
            }
        }
        if (Protocol.getProtocol().isOlderThanOrEqual(Protocol.v1_8_8)) {
            Object nmsItem = packetContainer.getPacket().getItem();

            if (nmsItem != null) {
                ItemStack bukkitItem = ItemHelper.asBukkitItem(nmsItem);

                if (bukkitItem != null) {
                    if (bukkitItem.getType() != itemInHand.getType()) {
                        VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this, "Illegal Placement");
                        packetContainer.cancel();
                        return;
                    }
                }
                if (NBTHelper.checkBukkitNBT(bukkitItem)) {
                    VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this, "Illegal NBT");
                    packetContainer.cancel();
                    return;
                }
                return;
            }
        }
        if (NBTHelper.checkBukkitNBT(itemInHand)) {
            VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this, "Illegal NBT");
            packetContainer.cancel();
        }
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.BLOCK_PLACE);
    }

    @Override
    public String getName() {
        return "Block Placement";
    }
}
