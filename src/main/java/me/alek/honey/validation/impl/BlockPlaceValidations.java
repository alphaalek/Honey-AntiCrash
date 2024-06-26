package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.ResponseContainer;
import me.alek.honey.validation.Validation;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInBlockPlace;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.statics.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class BlockPlaceValidations extends AbstractPacketListener<WrappedPlayInBlockPlace> {

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInBlockPlace>> getNewPipeline() {
        ValidationPipeline<PacketValidation<WrappedPlayInBlockPlace>> visitor = new ValidationPipeline<>();
        visitor.register(new BlockInteractA());
        visitor.register(new BlockInteractB());
        visitor.register(new BlockInteractC());
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)) {
            visitor.register(new BlockInteractD());
        }
        return visitor;
    }

    @Override
    public List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.BLOCK_PLACE);
    }

    public static ResponseContainer checkExceedingMaxDistance(HoneyPlayer player, WrappedBlockPosition blockPosition, int maxDistance, ValidationContextContainer.DevelopmentData developmentData) {

        // check if the block position is close to the origin block position, just ignore this packet if it is
        final Location originLocation = WrappedBlockPosition.ORIGIN.toLocation(player.getWorld());
        final Location lastPlayerLocation = MovementValidations.getLastLocationFor(player);

        if (blockPosition.distance(originLocation) < 10
                && LocationUtils.calculate3DDistance(lastPlayerLocation, originLocation) > 10) {
            return Validation.PASS;
        }

        // check if the chunk which contain that block is unloaded or not, because then you are not allowed to interact with
        // blocks in there
        if (!MovementValidations.isChunkLoaded(blockPosition.toLocation(player.getWorld()))
                || blockPosition.isInvalid())
            // dont flag because it might just be server lag, just cancel it
            return Validation.validate().cancel().build();

        // checks distance between the player and the block placed
        Location playerLocation = MovementValidations.getLastLocationFor(player);
        if (playerLocation == null) playerLocation = player.getLocation();

        final double distance = blockPosition.distance(playerLocation);

        // the distance is higher than the recent client location
        if (distance == -1 ||
                distance > maxDistance && !MovementValidations.isAnyLatelyTeleportationLocationsCloseTo(
                        player, maxDistance, blockPosition::distance)) {

            // add the distance to the development data
            return Validation.validate().cancel().flag()
                    .onViolation((check, onViolation) -> {
                        if (onViolation != null) onViolation.run();

                        developmentData.addPoint("BlockPos", blockPosition);
                        developmentData.addPoint("Distance", (distance == -1 ? "Invalid" : distance));
                    })
                    .build();
        }
        return Validation.PASS;
    }

    public static class BlockInteractA implements ViolationValidation<WrappedPlayInBlockPlace>, PacketValidation<WrappedPlayInBlockPlace> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInBlockPlace packet, ValidationContextContainer container) {
            if (getSetting().isEnabled()) {

                WrappedBlockPosition blockPosition = packet.getBlockPosition();
                if (blockPosition == null) return Validation.PASS;

                return checkExceedingMaxDistance(player, blockPosition, getSetting().max, container.getDevelopmentData(this));
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() { return "BlockInteract.A"; }

        @Override
        public String getDescription() {
            return Lang.get("validation.block.a");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.BlockInteract.BlockInteractA.class);
        }
    }

    public static class BlockInteractB implements PacketValidation<WrappedPlayInBlockPlace>, ViolationValidation<WrappedPlayInBlockPlace> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInBlockPlace packet, ValidationContextContainer container) {
            if (getSetting().isEnabled() && packet.getType() == PacketTypes.Play.Client.BLOCK_PLACE) {
                return ItemAnalysis.flagIf(player, packet.getItem(), container.getDevelopmentData(this));
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "BlockInteract.B (IA)";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation-block-b");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.BlockInteract.BlockInteractB.class);
        }
    }

    public static class BlockInteractC implements PacketValidation<WrappedPlayInBlockPlace>, ViolationValidation<WrappedPlayInBlockPlace> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInBlockPlace packet, ValidationContextContainer container) {
            if (getSetting().isEnabled() && packet.getType() == PacketTypes.Play.Client.BLOCK_PLACE) {
                ItemStack item = packet.getItem();
                if (item == null) {
                    return Validation.PASS;
                }

                Material material = item.getType();
                if (material == Material.PISTON_MOVING_PIECE
                        || material == Material.PISTON_EXTENSION) {
                    return Validation.validate().cancel().flag().build();
                }
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "BlockInteract.C";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation-block-c");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.BlockInteract.BlockInteractC.class);
        }
    }

    public static class BlockInteractD implements PacketValidation<WrappedPlayInBlockPlace>, ViolationValidation<WrappedPlayInBlockPlace> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ResponseContainer validate(HoneyPlayer player, WrappedPlayInBlockPlace packet, ValidationContextContainer container) {
            if (getSetting().isEnabled() && packet.getType() == PacketTypes.Play.Client.BLOCK_PLACE) {
                int placedBlockDirection = packet.getPlacedBlockDirection();

                float facingX = packet.getFacingX();
                float facingY = packet.getFacingY();
                float facingZ = packet.getFacingZ();

                ValidationContextContainer.DevelopmentData developmentData = container.getDevelopmentData(this);
                if (placedBlockDirection < 0 || (placedBlockDirection > 5 && placedBlockDirection != 255)) {
                    return Validation.validate().cancel().flag()
                            .onViolation((check, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                developmentData.addPoint("Block Direction", placedBlockDirection);
                            })
                            .build();
                }
                if (facingX < 0 || facingX > 1 || facingY < 0 || facingY > 1 || facingZ < 0 || facingZ > 1) {
                    return Validation.validate().cancel().flag()
                            .onViolation((check, onViolation) -> {
                                if (onViolation != null) onViolation.run();

                                developmentData.addPoint("Facing X", facingX);
                                developmentData.addPoint("Facing Y", facingY);
                                developmentData.addPoint("Facing Z", facingZ);
                            })
                            .build();
                }
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "BlockInteract.D";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation-block-d");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.BlockInteract.BlockInteractD.class);
        }
    }
}
