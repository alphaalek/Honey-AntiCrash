package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInBookEdit;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.MaterialMappings;
import me.alek.honey.validation.*;
import me.alek.honey.validation.item.ItemAnalysis;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class BookEditValidations extends AbstractValidationPacketListener<WrappedPlayInBookEdit> {

    public BookEditValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.INITIAL_BYTE, ListenerPriority.NORMAL, getPacketTypes());
    }

    private static List<PacketType> getPacketTypes() {
        return Collections.singletonList(PacketTypes.Play.Client.BOOK_EDIT);
    }

    @Override
    public ValidationPipeline<PacketValidation<WrappedPlayInBookEdit>> createValidationPipeline() {
        return ValidationPipeline.from(new BookEditA());
    }

    private static class BookEditA implements PacketValidation<WrappedPlayInBookEdit>, ViolationValidation<WrappedPlayInBookEdit> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WrappedPlayInBookEdit packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ItemAnalysis.flagIf(player, packet.getItemStack(), generateKey(), context)
                        .builder().onViolation(generateKey(), (innerKey, onViolation) -> {
                            if (onViolation != null) {
                                onViolation.run();
                            }
                            ItemStack heldItem = player.getItemInMainHand();
                            if (heldItem != null && heldItem.getType() == MaterialMappings.getTypeIfAny("writable-book")) {
                                heldItem.setType(Material.AIR);
                            }
                        }).build();
            }
            return ValidationViolationContext.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "BookEdit.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.book-edit.a");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.BookEdit.BookEditA.class);
        }
    }
}
