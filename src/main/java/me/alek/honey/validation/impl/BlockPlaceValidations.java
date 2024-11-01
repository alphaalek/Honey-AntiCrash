package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.validation.*;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.ValidationServiceContext;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInBlockPlace;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.statics.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class BlockPlaceValidations extends AbstractValidationPacketListener<WrappedPlayInBlockPlace> {

    public BlockPlaceValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.INITIAL_BYTE, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.BLOCK_PLACE);
    }

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInBlockPlace>> createValidationPipeline() {
        ValidationPipeline<PacketValidation<WrappedPlayInBlockPlace>> visitor = new ValidationPipeline<>();
        visitor.register(new BlockInteractA());
        visitor.register(new BlockInteractB());
        visitor.register(new BlockInteractC());
        return visitor;
    }

    public static ValidationServiceContext checkExceedingMaxDistance(HoneyPlayer player, ViolationKey key, WrappedBlockPosition blockPosition, int maxDistance, ValidationPipelineContext context) {

        // check if the block position is close to the origin block position, just ignore this packet if it is
        Location originLocation = WrappedBlockPosition.ORIGIN.toLocation(player.getWorld());
        Location lastPlayerLocation = MovementValidations.getLastLocationFor(player);

        if (blockPosition.distance(originLocation) < 10
                && LocationUtils.calculate3DDistance(lastPlayerLocation, originLocation) > 10) {
            return ValidationServiceContext.PASS;
        }

        // check if the chunk which contain that block is unloaded or not, because then you are not allowed to interact with
        // blocks in there
        if (!MovementValidations.isChunkLoaded(blockPosition.toLocation(player.getWorld()))
                || blockPosition.isInvalid()) {
            // dont flag because it might just be server lag, just cancel it
            return ValidationServiceContext.Builder.validate().cancel().build();
        }

        // checks distance between the player and the block placed
        Location playerLocation = MovementValidations.getLastLocationFor(player);
        if (playerLocation == null) {
            playerLocation = player.getLocation();
        }

        double distance = blockPosition.distance(playerLocation);

        // the distance is higher than the recent client location
        if (distance == -1 ||
                distance > maxDistance && !MovementValidations.isAnyLatelyTeleportationLocationsCloseTo(
                        player, maxDistance, blockPosition::distance)) {

            // add the distance to the development data
            return ValidationServiceContext.Builder.validate().cancel().flag()
                    .onViolation(key, (keyInner, onViolation) -> {
                        if (onViolation != null) onViolation.run();

                        context.getDevelopmentData(keyInner).addPoint("BlockPos", blockPosition);
                        context.getDevelopmentData(keyInner).addPoint("Distance", (distance == -1 ? "Invalid" : distance));
                    })
                    .build();
        }

        return ValidationServiceContext.PASS;
    }

    public static class BlockInteractA implements ViolationValidation<WrappedPlayInBlockPlace>, PacketValidation<WrappedPlayInBlockPlace> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInBlockPlace packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                WrappedBlockPosition blockPosition = packet.getBlockPosition();
                if (blockPosition == null) {
                    return ValidationServiceContext.PASS;
                }

                return checkExceedingMaxDistance(player, ViolationKey.of(this), blockPosition, getSetting().max, context);
            }

            return ValidationServiceContext.PASS;
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
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.BlockInteract.BlockInteractA.class);
        }
    }

    public static class BlockInteractB implements PacketValidation<WrappedPlayInBlockPlace>, ViolationValidation<WrappedPlayInBlockPlace> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInBlockPlace packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled() && packet.getType() == PacketTypes.Play.Client.BLOCK_PLACE) {
                return ItemAnalysis.flagIf(player, packet.getItemStack(), ViolationKey.of(this), context);
            }

            return ValidationServiceContext.PASS;
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
            return Lang.get("validation.block.b");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.BlockInteract.BlockInteractB.class);
        }
    }

    public static class BlockInteractC implements PacketValidation<WrappedPlayInBlockPlace>, ViolationValidation<WrappedPlayInBlockPlace> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPlayInBlockPlace packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled() && packet.getType() == PacketTypes.Play.Client.BLOCK_PLACE) {
                WrappedImmutableItemStack item = packet.getItemStack();
                if (item == null) {
                    return ValidationServiceContext.PASS;
                }

                Material material = item.getType();
                if (material.name().equals("PISTON_MOVING_PIECE")
                        || material.name().equals("PISTON_EXTENSION")) {
                    return ValidationServiceContext.Builder.validate().cancel().flag().build();
                }
            }

            return ValidationServiceContext.PASS;
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
            return Lang.get("validation.block.c");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.BlockInteract.BlockInteractC.class);
        }
    }
}
