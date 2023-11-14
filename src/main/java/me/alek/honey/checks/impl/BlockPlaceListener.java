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
import me.alek.honey.netty.packetwrappers.play.client.common.WrappedAbstractBlockInteraction;
import me.alek.honey.netty.utility.protocol.NetProtocol;
import me.alek.honey.utility.Locations;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class BlockPlaceListener<WP extends WrappedAbstractBlockInteraction<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WP> getVisitor() {
        CheckVisitor<WP> visitor = new CheckVisitor<>();
        visitor.visit(new BlockInteractA<>());
        visitor.visit(new BlockInteractB<>());
        visitor.visit(new BlockInteractC<>());
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            visitor.visit(new BlockInteractD<>());
        }
        visitor.visit(new BlockInteractF<>());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Arrays.asList(PacketType.Play.Client.BLOCK_PLACE, PacketType.Play.Client.USE_ITEM);
    }

    public static ResponseContainer checkExceedingMaxDistance(Player player, WrappedBlockPosition blockPosition, int maxDistance, CheckContextContainer.DevelopmentData developmentData) {

        // check if the block position is close to the origin block position, just ignore this packet if it is
        final Location originLocation = WrappedBlockPosition.ORIGIN.toLocation(player.getWorld());
        final Location lastPlayerLocation = PositionListener.getLastLocationFor(player);

        if (blockPosition.distance(originLocation) < 10
                && Locations.calculate3DDistance(lastPlayerLocation, originLocation) > 10) {
            return ResponseBuilder.SUCCESS;
        }

        // check if the chunk which contain that block is unloaded or not, because then you are not allowed to interact with
        // blocks in there
        if (!PositionListener.isChunkLoaded(blockPosition.toLocation(player.getWorld())))
            // dont flag because it might just be server lag, just cancel it
            return ResponseBuilder.inst().cancel().build();

        if (blockPosition.isInvalid())
            return ResponseBuilder.inst().cancel().flag().terminate().build();

        // checks distance between the player and the block placed
        Location playerLocation = PositionListener.getLastLocationFor(player);
        if (playerLocation == null) playerLocation = player.getLocation();

        final double distance = blockPosition.distance(playerLocation);

        // the distance is higher than the recent client location
        if (distance > maxDistance || distance == -1) {

            // let's now check the distance to the recent server locations, and see if that is also way off
            if (distance == -1
                    || !PositionListener.isAnyLatelyTeleportationLocationsCloseTo(
                            player, blockPosition::distance, maxDistance)) {
                // add the distance to the development data
                developmentData.addPoint("BlockPos", blockPosition);
                developmentData.addPoint("Distance", (distance == -1 ? "Invalid" : distance));
                return ResponseBuilder.inst().cancel().flag().build();
            }
        }
        return ResponseBuilder.SUCCESS;
    }

    public static class BlockInteractA<WP extends WrappedAbstractBlockInteraction<WP>> implements ViolationCheck, PacketCheck<WP> {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                WrappedBlockPosition blockPosition = packet.getBlockPosition();
                if (blockPosition == null) return ResponseBuilder.SUCCESS;

                return checkExceedingMaxDistance(player, blockPosition, getSetting().max, container.getDevelopmentData(this));
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() { return "BlockInteract.A"; }

        @Override
        public String getDescription() {
            return "Placing block too far away";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.BlockInteract.BlockInteractA.class);
        }
    }

    public static class BlockInteractB<WP extends WrappedAbstractBlockInteraction<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled() && packet.getType() == PacketType.Play.Client.BLOCK_PLACE) {
                final WrappedPlayInBlockPlace blockPlacePacket = (WrappedPlayInBlockPlace) packet;

                final ItemStack item = blockPlacePacket.getItem();
                final ItemStack itemInHand = player.getItemInHand();

                final CheckContextContainer.DevelopmentData developmentData = container.getDevelopmentData(this);
                if (ItemAnalysis.checkItem(player, item, developmentData) || ItemAnalysis.checkItem(player, itemInHand, developmentData)) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "BlockInteract.B (IA)";
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

    public static class BlockInteractC<WP extends WrappedAbstractBlockInteraction<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled() && packet.getType() == PacketType.Play.Client.BLOCK_PLACE) {
                final WrappedPlayInBlockPlace blockPlacePacket = (WrappedPlayInBlockPlace) packet;

                final ItemStack item = blockPlacePacket.getItem();
                if (item == null)
                    return ResponseBuilder.SUCCESS;

                final Material material = item.getType();

                if (material == Material.PISTON_MOVING_PIECE
                        || material == Material.PISTON_EXTENSION) {
                    return ResponseBuilder.inst().cancel().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "BlockInteract.C";
        }

        @Override
        public String getDescription() {
            return "Placing illegal piston block";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.BlockInteract.BlockInteractC.class);
        }
    }

    public static class BlockInteractD<WP extends WrappedAbstractBlockInteraction<WP>> implements PacketCheck<WP>, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled() && packet.getType() == PacketType.Play.Client.BLOCK_PLACE) {
                final WrappedPlayInBlockPlace blockPlacePacket = (WrappedPlayInBlockPlace) packet;

                final int placedBlockDirection = blockPlacePacket.getPlacedBlockDirection();

                final float facingX = blockPlacePacket.getFacingX();
                final float facingY = blockPlacePacket.getFacingY();
                final float facingZ = blockPlacePacket.getFacingZ();

                final CheckContextContainer.DevelopmentData developmentData = container.getDevelopmentData(this);

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
        public String getName() {
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

    public static class BlockInteractF<WP extends WrappedAbstractBlockInteraction<WP>> implements PacketCheck<WP>, ViolationCheck {

        private boolean isValidTileEntity(Material type) {
            final String name = type.name();
            return name.contains("CHEST") || name.contains("BARREL") || name.contains("SHULKER_BOX") || name.contains("ANVIL")
                    || name.contains("WORKBENCH") || name.contains("ENCHANTMENT_TABLE") || name.contains("FURNACE") || name.contains("NOTE_BLOCK")
                    || name.contains("JUKEBOX") || name.contains("HOPPER") || name.contains("DISPENSER") || name.contains("DROPPER")
                    || name.contains("GRINDSTONE") || name.contains("CARTOGRAPHY_TABLE") || name.contains("SMITHING_TABLE") || name.contains("FLETCHING_TABLE")
                    || name.contains("STONECUTTER") || name.contains("LECTERN") || name.contains("LOOM") || name.contains("BEACON")
                    || name.contains("BREWING_STAND");
        }

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting().isEnabled()) {

                final WrappedBlockPosition position = packet.getBlockPosition();
                if (position != null) {
                    final Location location = position.toLocation(player.getWorld());
                    if (!PositionListener.isChunkLoaded(location)) {
                        return ResponseBuilder.SUCCESS;
                    }
                    final Material type = location.getBlock().getType();
                    if (isValidTileEntity(type)) {
                        return ResponseBuilder.inst().flag().build();
                    }
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "BlockInteract.F";
        }

        @Override
        public String getDescription() {
            return "Interacting too quickly with certain tile entities";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.BlockInteract.BlockInteractF.class);
        }
    }
}
