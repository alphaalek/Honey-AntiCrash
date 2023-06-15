package me.alek.honey.checks.impl;

import me.alek.honey.checks.PacketCheck;
import me.alek.honey.checks.helper.NBTHelper;
import me.alek.honey.checks.helper.wrappers.models.WrappedBlockPosition;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInBlockPlace;
import me.alek.honey.netty.utility.protocol.Protocol;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class BlockPlacement extends AsyncPacketAdapter<WrappedPlayInBlockPlace> implements PacketCheck {

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
                flag(player, packetContainer, this, "Illegal Placement", ConfigurationOptions.BlockPlacement.get());
                return;
            }
        }
        if (Protocol.getProtocol().isOlderThanOrEqual(Protocol.v1_8_8)) {
            ItemStack item = packetContainer.getPacket().getItem();

            if (item != null) {
                if (item.getType() != itemInHand.getType()) {
                    punish(player, packetContainer, "Illegal Placement");
                    return;
                }
                if (NBTHelper.checkBukkitNBT(item)) {
                    punish(player, packetContainer, "Illegal NBT");
                    return;
                }
                return;
            }
        }
        if (NBTHelper.checkBukkitNBT(itemInHand)) {
            punish(player, packetContainer, "Illegal NBT");
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
