package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.*;
import me.alek.honey.validation.ValidationServiceContext;
import me.alek.honey.validation.types.KeyedViolationValidation;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInSetCreativeSlot;
import me.alek.honey.services.ViolationService;
import me.alek.honey.util.Callback;
import me.alek.honey.util.IDRegistry;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class FloodValidations<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> {

    public static final ValidationPipeline<ViolationValidation.IntViolation> TOO_EARLY_DROP_PIPELINE = ValidationPipeline.from(new FloodE());

    private static final Map<UUID, Integer> lastSetCreativeSlot = new HashMap<>();

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, ValidationPipelineContext context, Callback callback) {

        // ignore set creative slot packets when players in creative mode clear their inventory
        if (packetContainer.getType() == PacketTypes.Play.Client.CREATIVE_INVENTORY_ACTION
                && player.player.getGameMode() == GameMode.CREATIVE) {
            int slot = ((WrappedPlayInSetCreativeSlot)packetContainer.getPacket()).getSlot();
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
    public ListenerPriority getPriority() {
        return ListenerPriority.LOW;
    }

    @Override
    public ValidationPipeline<PacketValidation<WP>> getNewPipeline() {
        return ValidationPipeline.from(new FloodA<>(), new FloodB<>(), new FloodC<>(), new FloodD<>());
    }

    @Override
    public List<PacketType> getPacketTypes() {
        Set<PacketType> floodSettings = ConfigurationOptions.getFloodSettings().keySet();

        List<PacketType> packetTypes = new ArrayList<>();
        for (PacketType packetType : PacketTypes.getTypesAvailableFor(PacketState.PLAY, PacketBound.SERVER)) {
            if (floodSettings.contains(packetType)) {
                packetTypes.add(packetType);
            }
        }

        return packetTypes;
    }

    public static abstract class AbstractCustomizableFloodCheck<WP extends WrappedPacket<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag().build();
            }

            return ValidationServiceContext.PASS;
        }
    }

    public static class FloodA<WP extends WrappedPacket<WP>> extends AbstractCustomizableFloodCheck<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.PacketFlood.FloodA.class);
        }

        @Override
        public int getID() {
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

    public static class FloodB<WP extends WrappedPacket<WP>> extends AbstractCustomizableFloodCheck<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.PacketFlood.FloodB.class);
        }

        @Override
        public int getID() {
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

    public static class FloodC<WP extends WrappedPacket<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final IDRegistry packetTypes = new IDRegistry();

        static {
            for (PacketType packetType : PacketTypes.getTypesAvailable()) {
                if (!ReflectStructureCache.acquireStructure(packetType.getNmsClass()).withType(NMS.getItemStackClass())
                        .getAccessors().isEmpty()) {
                    packetTypes.add(packetType.getHoneyID());
                }
            }
        }

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()
                    && !PacketTypes.isUnknown(packet.getType())
                    && packetTypes.hasID(packet.getType().getHoneyID())) {

                for (ItemStack item : packet.getContainer().getItems().readFieldValues()) {
                    NBTCompound nbt = NBTFactory.fromItem(item, false, true);
                    if (nbt != null && !nbt.isEmpty()) {
                        int elements = nbt.getRoot().getTotalElements();
                        return ValidationServiceContext.Builder.validate().flag(elements).build();
                    }
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
            return "Flood.C";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.flood.c");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.PacketFlood.FloodC.class);
        }
    }

    public static class FloodD<WP extends WrappedPacket<WP>> implements PacketValidation<WP>, KeyedViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting(packet).isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag().withViolationKey(getKey(packet)).build();
            }

            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
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
        public ViolationService.HoneyKey getKey(WP val) {
            return ViolationService.HoneyKey.of(val.getType().getHoneyID());
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting(WP packet) {
            return ConfigurationOptions.getFloodSettings().get(packet.getType());
        }
    }

    public static class FloodE implements ViolationValidation.IntViolation {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public int getID() {
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.PacketFlood.FloodE.class);
        }
    }
}
