package me.alek.honey.checks.impl;

import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.item.ItemAnalysis;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInBlockPlace;
import me.alek.honey.netty.utility.protocol.Protocol;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class BlockPlaceListener extends AbstractPacketListener<WrappedPlayInBlockPlace> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInBlockPlace> packetContainer, CheckContextContainer container) {
        final ItemStack itemInHand = player.getItemInHand();
        return itemInHand.getType() != Material.AIR;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInBlockPlace> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedPlayInBlockPlace> getVisitor() {
        CheckVisitor<WrappedPlayInBlockPlace> visitor = new CheckVisitor<>();
        visitor.visit(new BlockInteractA());
        visitor.visit(new BlockInteractB());
        visitor.visit(new BlockInteractC());
        if (Protocol.getProtocol().isOlderThanOrEqual(Protocol.v1_8_8)) {
            visitor.visit(new BlockInteractD());
        }
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.BLOCK_PLACE);
    }

    public static ResponseContainer checkExceedingMaxDistance(Player player, WrappedBlockPosition blockPosition, int maxDistance, CheckContextContainer.DevelopmentData developmentData) {

        if (blockPosition.equals(WrappedBlockPosition.ORIGIN) || blockPosition.equals(WrappedBlockPosition.DEFAULT))
            return ResponseBuilder.SUCCESS;

        // check if the chunk which contain that block is unloaded or not, because then you are not allowed to interact with
        // blocks in there
        if (!PositionListener.isChunkLoaded(blockPosition.toLocation(player.getWorld())))
            return ResponseBuilder.inst().cancel().build();

        if (blockPosition.isInvalid())
            return ResponseBuilder.inst().cancel().flag().terminate().build();

        // checks distance between the player and the block placed
        Location playerLocation = PositionListener.getLastLocationFor(player);
        if (playerLocation == null) playerLocation = player.getLocation();

        double distance = blockPosition.distance(playerLocation);

        // the distance is higher than the recent client location
        if (distance > maxDistance || distance == -1) {

            // let's now check the distance to the recent server locations, and see if that is also way off
            if (distance != -1 && !PositionListener.isAnyLatelyTeleportationLocationsCloseTo(player, blockPosition.toLocation(player.getWorld()), maxDistance)) {

                // add the distance to the development data
                developmentData.addPoint("BlockPos", blockPosition);
                developmentData.addPoint("Distance", (distance == -1 ? "-1 (Invalid BlockPos)" : distance));
                return ResponseBuilder.inst().cancel().flag().build();
            }
        }
        return ResponseBuilder.SUCCESS;
    }

    public static class BlockInteractA implements ViolationCheck, PacketCheck<WrappedPlayInBlockPlace> {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInBlockPlace packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                WrappedBlockPosition blockPosition = packet.getBlockPosition();
                if (blockPosition == null) return ResponseBuilder.SUCCESS;

                return checkExceedingMaxDistance(player, blockPosition, ConfigurationOptions.BlockInteract.BlockInteractA.MAX, container.getDevelopmentData(this));
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() { return "BlockInteract.A"; }

        @Override
        public String getDescription() {
            return "Placing block too far away";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.BlockInteract.BlockInteractA.class);
        }
    }

    public static class BlockInteractB implements PacketCheck<WrappedPlayInBlockPlace>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInBlockPlace packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ItemStack item = packet.getItem();
                ItemStack itemInHand = player.getItemInHand();

                if (ItemAnalysis.checkItem(player, item) || ItemAnalysis.checkItem(player, itemInHand)) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "BlockInteract.B";
        }

        @Override
        public String getDescription() {
            return "Right clicking with illegal item";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.BlockInteract.BlockInteractB.class);
        }
    }

    public static class BlockInteractC implements PacketCheck<WrappedPlayInBlockPlace>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInBlockPlace packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                ItemStack item = packet.getItem();
                if (item == null) {
                    return ResponseBuilder.SUCCESS;
                }
                Material material = item.getType();
                if (
                    material == Material.PISTON_MOVING_PIECE || material == Material.PISTON_EXTENSION
                ) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "BlockInteract.C";
        }

        @Override
        public String getDescription() {
            return "Placing illegal block";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.BlockInteract.BlockInteractC.class);
        }
    }

    public static class BlockInteractD implements PacketCheck<WrappedPlayInBlockPlace>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WrappedPlayInBlockPlace packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                final int placedBlockDirection = packet.getPlacedBlockDirection();

                final float facingX = packet.getFacingX();
                final float facingY = packet.getFacingY();
                final float facingZ = packet.getFacingZ();

                CheckContextContainer.DevelopmentData developmentData = container.getDevelopmentData(this);

                if (placedBlockDirection < 0 || (placedBlockDirection > 5 && placedBlockDirection != 255)) {
                    developmentData.addPoint("Block Direction", placedBlockDirection);
                    return ResponseBuilder.inst().cancel().flag().build();
                }
                if (facingX < 0 || facingX > 1 || facingY < 0 || facingY > 1 || facingZ < 0 || facingZ > 1) {
                    developmentData.addPoint("Facing X", facingX);
                    developmentData.addPoint("Facing Y", facingY);
                    developmentData.addPoint("Facing Z", facingZ);
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "BlockInteract.D";
        }

        @Override
        public String getDescription() {
            return "Illegal facing or direction";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.BlockInteract.BlockInteractD.class);
        }
    }
}
