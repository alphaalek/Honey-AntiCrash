package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.validation.*;
import me.alek.honey.validation.ValidationServiceContext;
import me.alek.honey.validation.types.HoneyValidation;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketReader;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInLook;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInPosition;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInPositionLook;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.util.Array;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.util.statics.NumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class NettyValidations<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> {

    public static final ValidationPipeline<ViolationValidation.IntViolation> PRINT_FILTER_PIPELINE = ValidationPipeline.from(new NettyA());

    @Override
    public List<PacketType> getPacketTypes() {
        return PacketTypes.getTypesAvailableFor(PacketState.PLAY, PacketBound.SERVER).stream().collect(Collectors.toList());
    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.HIGHEST;
    }

    @Override
    public ValidationPipeline<PacketValidation<WP>> getNewPipeline() {
        return ValidationPipeline.from(
                /* new NettyA<>(), */ new NettyB<>(), /*  new NettyC<>(), */ new NettyH<>(), new NettyI<>() /*, new NettyJ<>() */);
    }

    public static class NettyA implements ViolationValidation.IntViolation {
        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.a");
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyA.class);
        }
    }

    public static class NettyB<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();
        private static final List<PacketType> IGNORE = Collections.singletonList(PacketTypes.Play.Client.PLAYER_MOVEMENT);
        private static final Array<PacketReader.Container<? extends WrappedPacket<?>>> READERS = new Array<>();

        static {
            READERS.set(PacketTypes.Play.Client.PLAYER_POSITION.getHoneyID(), new PacketReader.Container<WrappedPlayInPosition>() {

                @Override
                public Object[] read(PacketContainer<WrappedPlayInPosition> packetContainer) {
                    return new Object[]{
                            packetContainer.getPacket().getX(),
                            packetContainer.getPacket().getY(),
                            packetContainer.getPacket().getZ()};
                }
            });
            READERS.set(PacketTypes.Play.Client.PLAYER_POS_AND_ROT.getHoneyID(), new PacketReader.Container<WrappedPlayInPositionLook>() {

                @Override
                public Object[] read(PacketContainer<WrappedPlayInPositionLook> packetContainer) {
                    return new Object[]{
                            packetContainer.getPacket().getX(),
                            packetContainer.getPacket().getY(),
                            packetContainer.getPacket().getZ(),
                            packetContainer.getPacket().getYaw(),
                            packetContainer.getPacket().getPitch()};
                }
            });
            READERS.set(PacketTypes.Play.Client.PLAYER_ROTATION.getHoneyID(), new PacketReader.Container<WrappedPlayInLook>() {

                @Override
                public Object[] read(PacketContainer<WrappedPlayInLook> packetContainer) {
                    return new Object[]{
                            packetContainer.getPacket().getYaw(),
                            packetContainer.getPacket().getPitch()};
                }
            });
        }

        @Override
        public ValidationPipeline<PacketValidation<WP>> getNewPipeline() {
            return ValidationPipeline.from(this);
        }

        @Override
        public List<PacketType> getPacketTypes() {
            List<PacketType> packetTypes = new ArrayList<>();

            BiPredicate<ReflectStructure<?, ?>, Class<?>> structureClass = (structure, clazz) ->
                    !structure.withType(clazz).withoutStatic().getAccessors().isEmpty();
            for (PacketType packetType : PacketTypes.getTypesAvailableForBound(PacketBound.SERVER)) {
                ReflectStructure<Object, ?> structure = ReflectStructureCache.acquireStructure(packetType);
                if (structureClass.test(structure, double.class) || structureClass.test(structure, float.class)) {
                    packetTypes.add(packetType);
                }
            }
            packetTypes.removeAll(IGNORE);

            return packetTypes;
        }

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {

                PacketReader.Container<WP> reader = (PacketReader.Container<WP>) READERS.get(packet.getType().getHoneyID());
                if (reader != null) {
                    Object[] values = reader.read(packet.getContainer());
                    for (Object value : values) {
                        if (value instanceof Float && NumberUtils.isInvalid((float) value)) {
                            return ValidationServiceContext.FLAG;
                        }
                        if (value instanceof Double && NumberUtils.isInfinite((double) value)) {
                            return ValidationServiceContext.FLAG;
                        }
                    }

                    return ValidationServiceContext.PASS;
                }

                for (double value : packet.getContainer().getDoubles().readFieldValues()) {
                    if (NumberUtils.isInvalid(value)) {
                        return ValidationServiceContext.FLAG;
                    }
                }

                for (float value : packet.getContainer().getFloats().readFieldValues()) {
                    if (NumberUtils.isInvalid(value)) {
                        return ValidationServiceContext.FLAG;
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
            return "Netty.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.b");
        }

        @Override
        public ListenerPriority getPriority() {
            return ListenerPriority.HIGHEST;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyB.class);
        }
    }

    public static class NettyC<WP extends WrappedPacket<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.C";
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyC.class);
        }
    }

    public static class NettyH<WP extends WrappedPacket<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        public static <WP extends WrappedPacket<WP>> ValidationServiceContext check0(WP packet, ViolationKey key, ConfigurationOptions.VlSetting setting, ValidationPipelineContext context) {
            double decodeTime = packet.getContainer().getEventloopStateTimings().getTimeForMillis(EventloopState.DECODE);
            if (setting.isEnabled() && decodeTime > 0) {
                return ValidationServiceContext.Builder.validate().flag(decodeTime)
                        .onViolation(key, (keyInner, onViolation) -> {
                            if (onViolation != null) onViolation.run();

                            context.getDevelopmentData(key).addPoint("Decode Time", NumberUtils.format(decodeTime) + " ms");
                        })
                        .build();
            }

            return ValidationServiceContext.PASS;
        }

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            return check0(packet, ViolationKey.of(this), getSetting(), context);
        }

        @Override
        public int getID() {
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyH.class);
        }
    }

    public static class NettyI<WP extends WrappedPacket<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            return NettyH.check0(packet, ViolationKey.of(this), getSetting(), context);
        }

        @Override
        public int getID() {
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
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyI.class);
        }
    }

    public static class NettyJ<WP extends WrappedPacket<WP>> implements PacketValidation<WP>, ViolationValidation<WP> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WP packet, ValidationPipelineContext context) {
            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.J";
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyJ.class);
        }
    }
}
