package me.alek.honey.validation.impl;

import me.alek.honey.HoneyKey;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.NBTFactory;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInSetCreativeSlot;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.reflection.MinecraftReflection;
import me.alek.honey.validation.*;
import me.alek.honey.validation.KeyedVlValidation;
import me.alek.honey.validation.PacketValidation;
import me.alek.honey.validation.ViolationValidation;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class FloodValidations<WP extends SourcedWrappedPacket<WP>> extends AbstractValidationPacketListener<WP> {

    public static final ValidationPipeline<ViolationValidation.IntViolation> PACKET_DROP_PIPELINE = ValidationPipeline.from(new FloodE());
    private static final Map<UUID, Integer> lastSetCreativeSlot = new HashMap<>();

    public FloodValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.LOW, getPacketTypes());
    }

    public static List<PacketType> getPacketTypes() {
        Set<PacketType> floodSettings = ConfigurationOptions.getFloodSettings().keySet();

        List<PacketType> types = new ArrayList<>();
        for (PacketType packetType : PacketTypes.getTypesAvailableFor(PacketState.PLAY, PacketBound.SERVER)) {
            if (floodSettings.contains(packetType)) {
                types.add(packetType);
            }
        }

        return types;
    }

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packet, ValidationPipelineContext context) {
        // ignore set creative slot packets when players in creative mode clear their inventory
        if (packet.getType() == PacketTypes.Play.Client.CREATIVE_INVENTORY_ACTION
                && player.bukkit.getGameMode() == GameMode.CREATIVE) {
            int slot = ((WrappedPlayInSetCreativeSlot) packet.getAnyWrapped()).getSlot();
            int prevSlot = lastSetCreativeSlot.computeIfAbsent(player.uuid, (d) -> slot);

            lastSetCreativeSlot.put(player.uuid, slot);
            if (prevSlot + 1 == slot) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void cleanup(HoneyPlayer player) {
        lastSetCreativeSlot.remove(player.uuid);
    }

    @Override
    public ValidationPipeline<PacketValidation<WP>> createValidationPipeline() {
        return ValidationPipeline.from(new FloodA<>(), new FloodB<>(), new FloodC<>(), new FloodD<>());
    }

    public static abstract class AbstractCustomizableFloodCheck<WP extends SourcedWrappedPacket<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationViolationContext.Builder.validate().flag().build();
            }

            return ValidationViolationContext.PASS;
        }
    }

    public static class FloodA<WP extends SourcedWrappedPacket<WP>> extends AbstractCustomizableFloodCheck<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.PacketFlood.FloodA.class);
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Flood.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.flood.a");
        }
    }

    public static class FloodB<WP extends SourcedWrappedPacket<WP>> extends AbstractCustomizableFloodCheck<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.PacketFlood.FloodB.class);
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Flood.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.flood.b");
        }
    }

    public static class FloodC<WP extends SourcedWrappedPacket<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();
        private static final IDRegistry packetTypes = new IDRegistry();

        static {
            for (PacketType type : PacketTypes.getTypesAvailable()) {
                if (!ReflectStructure.Provider.acquireStructure(type.getNmsClass())
                        .withType(MinecraftReflection.getItemStackClass())
                        .getAccessors()
                        .isEmpty()) {
                    packetTypes.add(type.getHoneyID());
                }
            }
        }

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()
                    && !PacketTypes.isUnknown(packet.getType())
                    && packetTypes.hasID(packet.getType().getHoneyID())) {

                for (ItemStack item : packet.getContainer().getStructure().getItems().readFieldValues()) {
                    CompoundTag nbt = NBTFactory.fromItem(item, false);
                    if (nbt != null && !nbt.isEmpty()) {
                        return ValidationViolationContext.Builder.validate().flag(NBTFactory.getAmountOfTags(nbt)).build();
                    }
                }
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Flood.C";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.flood.c");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.PacketFlood.FloodC.class);
        }
    }

    public static class FloodD<WP extends SourcedWrappedPacket<WP>> implements PacketValidation<WP>, KeyedVlValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting(packet).isEnabled()) {
                return ValidationViolationContext.Builder.validate().flag().withViolationKey(getKey(packet)).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Flood.D";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.flood.d");
        }

        @Override
        public HoneyKey getKey(WP val) {
            return HoneyKey.of(val.getType().getHoneyID());
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting(WP packet) {
            return ConfigurationOptions.getFloodSettings().get(packet.getType());
        }
    }

    public static class FloodE implements ViolationValidation.IntViolation {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Flood.E";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.flood.d");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.PacketFlood.FloodE.class);
        }
    }
}
