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
import org.bukkit.Bukkit;
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
        visitor.visit(new PlaceB());
        visitor.visit(new PlaceC());
        if (Protocol.getProtocol().isOlderThanOrEqual(Protocol.v1_8_8)) {
            visitor.visit(new PlaceD());
        }
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
            if (blockPosition == null) {
                return violate();
            }
            if (!blockPosition.equals(WrappedBlockPosition.DEFAULT)) {

                double distance = blockPosition.distance(player.getLocation());
                if (distance > ConfigurationOptions.BlockPlacement.MAX_BLOCK_TO_PLAYER_DISTANCE) {
                    return violate();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        private ResponseContainer violate() {
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .flag()
                    .build();
        }

        @Override
        public String getViolationName() {
            return "PlaceA";
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

        @Override
        public String getViolationName() {
            return "PlaceB";
        }
    }

    public static class PlaceC implements PacketCheck<WrappedPlayInBlockPlace> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInBlockPlace packet, CheckContextContainer container) {
            ItemStack item = packet.getItem();
            if (item == null) {
                return ResponseBuilder.SUCCESS;
            }
            Material material = item.getType();
            if (material == Material.PISTON_MOVING_PIECE || material == Material.PISTON_EXTENSION) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .punish()
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "PlaceC";
        }
    }

    public static class PlaceD implements PacketCheck<WrappedPlayInBlockPlace> {

        @Override
        public ResponseContainer accept(Player player, WrappedPlayInBlockPlace packet, CheckContextContainer container) {
            final int placedBlockDirection = packet.getPlacedBlockDirection();
            final float facingX = packet.getFacingX();
            final float facingY = packet.getFacingY();
            final float facingZ = packet.getFacingZ();

            if (placedBlockDirection < 0 || (placedBlockDirection > 5 && placedBlockDirection != 255)) {
                return violate();
            }
            if (facingX < 0 || facingX > 1 || facingY < 0 || facingY > 1 || facingZ < 0 || facingZ > 1) {
                return violate();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "PlaceD";
        }

        private ResponseContainer violate() {
            return ResponseBuilder
                    .inst()
                    .cancel()
                    .punish()
                    .build();
        }
    }
}
