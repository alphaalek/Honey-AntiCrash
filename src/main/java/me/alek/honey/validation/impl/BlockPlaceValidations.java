package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInBlockPlace;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.statics.LocationUtils;
import me.alek.honey.validation.*;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.PacketValidation;
import me.alek.honey.validation.ViolationValidation;
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
        return visitor;
    }

    public static ValidationViolationContext checkExceedingMaxDistance(HoneyPlayer player, ViolationKey key, WrappedBlockPosition blockPosition, int maxDistance, ValidationPipelineContext context) {
        // check if the block position is close to the origin block position, just ignore this packet if it is
        Location originLocation = WrappedBlockPosition.ORIGIN.toLocation(player.getWorld());
        Location lastPlayerLocation = MovementValidations.INSTANCE.getLastLocationFor(player);

        if (blockPosition.distance(originLocation) < 10
                && LocationUtils.calculate3DDistance(lastPlayerLocation, originLocation) > 10) {
            return ValidationViolationContext.PASS;
        }

        // check if the chunk which contain that block is unloaded or not, because then you are not allowed to interact with
        // blocks in there
        if (!MovementValidations.isChunkLoaded(blockPosition.toLocation(player.getWorld()))
                || blockPosition.isInvalid()) {
            // dont flag because it might just be server lag, just cancel it
            return ValidationViolationContext.Builder.validate().cancel().build();
        }

        // checks distance between the player and the block placed
        Location playerLocation = MovementValidations.INSTANCE.getLastLocationFor(player);
        if (playerLocation == null) {
            playerLocation = player.getLocation();
        }

        double distance = blockPosition.distance(playerLocation);

        // the distance is higher than the recent client location
        if (distance == -1 ||
                distance > maxDistance && !MovementValidations.INSTANCE.isAnyLatelyTeleportationLocationsCloseTo(
                        player, maxDistance, blockPosition::distance)) {

            // add the distance to the development data
            return ValidationViolationContext.Builder.validate().cancel().flag()
                    .onViolation(key, (keyInner, onViolation) -> {
                        if (onViolation != null) onViolation.run();

                        context.getDevelopmentData(keyInner).addPoint("Position", blockPosition);
                        context.getDevelopmentData(keyInner).addPoint("Distance", (distance == -1 ? "Invalid" : distance));
                    })
                    .build();
        }

        return ValidationViolationContext.PASS;
    }

    public static class BlockInteractA implements ViolationValidation<WrappedPlayInBlockPlace>, PacketValidation<WrappedPlayInBlockPlace> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInBlockPlace packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                WrappedBlockPosition blockPosition = packet.getBlockPosition();
                if (blockPosition == null) {
                    return ValidationViolationContext.PASS;
                }

                // TODO
                // return checkExceedingMaxDistance(player, generateKey(), blockPosition, getSetting().max, context);
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInBlockPlace packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ItemAnalysis.flagIf(player, packet.getItemStack(), generateKey(), context)
                        .builder().onViolation(generateKey(), (keyInner, onViolation) -> {
                            if (onViolation != null) {
                                onViolation.run();
                            }
                            ItemStack heldItem = player.getItemInMainHand();
                            if (heldItem != null && packet.getItemStack().isSimilar(heldItem)) {
                                heldItem.setType(Material.AIR);
                            }
                        }).build();

            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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
}
