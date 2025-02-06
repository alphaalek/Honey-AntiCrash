package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.PacketReader;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInLook;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInPosition;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInPositionLook;
import me.alek.honey.netty.processor.ListenerPriority;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.Array;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.statics.NumberUtils;
import me.alek.honey.validation.*;
import me.alek.honey.validation.PacketValidation;
import me.alek.honey.validation.ViolationValidation;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NettyValidations<WP extends SourcedWrappedPacket<WP>> extends AbstractValidationPacketListener<WP> {

    public NettyValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.HIGH, getPacketTypes());
    }

    public static List<PacketType> getPacketTypes() {
        return PacketTypes.getTypesAvailableFor(PacketState.PLAY, PacketBound.SERVER).stream().collect(Collectors.toList());
    }

    @Override
    public ValidationPipeline<PacketValidation<WP>> createValidationPipeline() {
        return ValidationPipeline.from(new NettyH<>(), new NettyI<>());
    }

    public static class NettyB<WP extends SourcedWrappedPacket<WP>> extends AbstractValidationPacketListener<WP> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        private static final Array<PacketReader.Container<?>> READERS = new Array<>();

        static {
            READERS.set(PacketTypes.Play.Client.PLAYER_POSITION, (PacketReader.Container<WrappedPlayInPosition>) packet -> {
                Location loc = packet.getAnyWrapped().getLocation(packet.getPlayer().getWorld());
                return new Object[]{
                        loc.getX(),
                        loc.getY(),
                        loc.getZ()
                };
            });
            READERS.set(PacketTypes.Play.Client.PLAYER_POS_ROT, (PacketReader.Container<WrappedPlayInPositionLook>) packet -> {
                Location loc = packet.getAnyWrapped().getLocation(packet.getPlayer().getWorld());
                return new Object[]{
                        loc.getX(),
                        loc.getY(),
                        loc.getZ(),
                        loc.getYaw(),
                        loc.getPitch()
                };
            });
            READERS.set(PacketTypes.Play.Client.PLAYER_ROTATION, (PacketReader.Container<WrappedPlayInLook>) packet -> {
                Location loc = packet.getAnyWrapped().getLocation(packet.getPlayer().getWorld());
                return new Object[]{
                        loc.getYaw(),
                        loc.getPitch()
                };
            });
        }

        public NettyB(PacketLibrary library) {
            super(library, PacketContainer.PipelineState.OBJECT, ListenerPriority.HIGHEST, getPacketTypes());
        }

        @Override
        public ValidationPipeline<PacketValidation<WP>> createValidationPipeline() {
            return ValidationPipeline.from(this);
        }

        public static List<PacketType> getPacketTypes() {
            List<PacketType> types = new ArrayList<>();

            for (PacketType type : PacketTypes.getTypesAvailableForBound(PacketBound.SERVER)) {
                ReflectStructure<Object, ?> structure = new ReflectStructure<>(type.getNmsClass());
                if (!structure.withDoubles().withStatic(false).withSuperClass(false).getAccessors().isEmpty()
                        || !structure.withFloats().withStatic(false).withSuperClass(false).getAccessors().isEmpty()) {
                    types.add(type);
                }
            }

            return types;
        }

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {

                PacketReader.Container<WP> reader = (PacketReader.Container<WP>) READERS.get(packet.getType().getHoneyID());
                if (reader != null) {
                    Object[] values = reader.read(packet.getContainer());
                    for (Object value : values) {
                        if (value instanceof Float && NumberUtils.isInvalid((float) value)) {
                            return ValidationViolationContext.FLAG;
                        }
                        if (value instanceof Double && NumberUtils.isInfinite((double) value)) {
                            return ValidationViolationContext.FLAG;
                        }
                    }

                    return ValidationViolationContext.PASS;
                }

                for (double value : packet.getContainer().getStructure().getDoubles().readFieldValues()) {
                    if (NumberUtils.isInvalid(value)) {
                        return ValidationViolationContext.FLAG;
                    }
                }

                for (float value : packet.getContainer().getStructure().getFloats().readFieldValues()) {
                    if (NumberUtils.isInvalid(value)) {
                        return ValidationViolationContext.FLAG;
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
            return "Netty.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.b");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyB.class);
        }
    }

    public static class NettyH<WP extends SourcedWrappedPacket<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        public static <WP extends SourcedWrappedPacket<WP>> ValidationViolationContext check0(WP packet, ViolationKey key, ConfigurationOptions.ViolationSetting setting, ValidationPipelineContext context) {
            double decodeTime = packet.getContainer().getEventloopStateTimings().getTimeForMillis(LifecycleState.DECODE);
            if (setting.isEnabled() && decodeTime > 0) {
                return ValidationViolationContext.Builder.validate().flag(decodeTime)
                        .onViolation(key, (keyInner, onViolation) -> {
                            if (onViolation != null) onViolation.run();

                            context.getDevelopmentData(key).addPoint("Decode Time", NumberUtils.format(decodeTime) + " ms");
                        })
                        .build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            return check0(packet, ViolationKey.of(this), getSetting(), context);
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.H";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.h");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyH.class);
        }
    }

    public static class NettyI<WP extends SourcedWrappedPacket<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            return NettyH.check0(packet, ViolationKey.of(this), getSetting(), context);
        }

        @Override
        public Integer getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.I";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.i");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyI.class);
        }
    }
}
