package me.alek.honey.checks.listeners;

import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.helper.NBTHelper;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.IntervalCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
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

public class BlockPlace extends AbstractPacketListener<WrappedPlayInBlockPlace> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInBlockPlace> packetContainer, CheckContextContainer container) {
        final ItemStack itemInHand = player.getItemInHand();
        return itemInHand.getType() != Material.AIR;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInBlockPlace> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedPlayInBlockPlace> getVisitor() {
        CheckVisitor<WrappedPlayInBlockPlace> visitor = new CheckVisitor<>();
        visitor.visit(new PlaceA());
        if (Protocol.getProtocol().isOlderThanOrEqual(Protocol.v1_8_8)) {
            visitor.visit(new PlaceB());
        }
        visitor.visit(new PlaceC());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.BLOCK_PLACE);
    }

    public static class PlaceA implements IntervalCheck, PacketCheck<WrappedPlayInBlockPlace> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInBlockPlace packet, CheckContextContainer container) {
            WrappedBlockPosition blockPosition = packet.getBlockPosition();
            if (!blockPosition.equals(WrappedBlockPosition.DEFAULT)) {

                double distance = blockPosition.distance(player.getLocation());
                if (distance > ConfigurationOptions.BlockPlacement.MAX_BLOCK_TO_PLAYER_DISTANCE) {
                    return ResponseBuilder
                            .inst()
                            .cancel()
                            .flag()
                            .build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public ConfigurationOptions.IntervalSetting getSetting() {
            return ConfigurationOptions.BlockPlacement.get();
        }
    }

    public static class PlaceB implements PacketCheck<WrappedPlayInBlockPlace> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInBlockPlace packet, CheckContextContainer container) {
            ItemStack item = packet.getItem();
            ItemStack itemInHand = player.getItemInHand();

            if (item.getType() != itemInHand.getType()) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }
    }

    public static class PlaceC implements PacketCheck<WrappedPlayInBlockPlace> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInBlockPlace packet, CheckContextContainer container) {
            ItemStack item = packet.getItem();
            ItemStack itemInHand = player.getItemInHand();

            if (NBTHelper.checkBukkitNBT(item) || NBTHelper.checkBukkitNBT(itemInHand)) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .confiscate(item)
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }
    }
}
