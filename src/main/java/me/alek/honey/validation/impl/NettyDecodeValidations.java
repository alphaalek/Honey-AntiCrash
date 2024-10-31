package me.alek.honey.validation.impl;

import io.netty.buffer.ByteBuf;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.validation.*;
import me.alek.honey.validation.ValidationServiceContext;
import me.alek.honey.validation.types.PacketValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.util.counter.DecayingListeningCounter;
import me.alek.honey.util.IDRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class NettyDecodeValidations<WP extends WrappedPacket<WP>> extends AbstractValidationPacketListener<WP> {

    public NettyDecodeValidations(PacketLibrary library) {
        super(library, PacketContainer.PipelineState.INITIAL_BYTE, ListenerPriority.NORMAL, getPacketTypes());
    }

    public static List<PacketType> getPacketTypes() {
        return PacketTypes.getTypesAvailableFor(PacketState.PLAY, PacketBound.SERVER).stream().collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValidationPipeline<PacketValidation<WP>> createValidationPipeline() {
        return (ValidationPipeline<PacketValidation<WP>>) (ValidationPipeline<?>) ValidationPipeline.from(new NettyD(), new NettyE(), new NettyF(), new NettyG(), new NettyK(), new NettyL(), new NettyM(), new NettyN(), new NettyO());
    }

    public static class NettyD implements PacketValidation.Unknown, ViolationValidation<WrappedPacket<?>> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPacket<?> packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag(packet.getContainer().getInitialByteWrapped().getByteSize()).build();
            }

            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.D";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.d");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyD.class);
        }
    }

    public static class NettyE implements PacketValidation.Unknown, ViolationValidation<WrappedPacket<?>> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPacket<?> packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag(packet.getContainer().getInitialByteWrapped().getByteSize()).build();
            }

            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.E";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.e");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyE.class);
        }
    }

    public static class NettyF implements PacketValidation.Unknown, ViolationValidation<WrappedPacket<?>> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPacket<?> packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag(packet.getContainer().getInitialByteWrapped().getAccumulatedByteSize()).build();
            }

            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.F";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.f");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyF.class);
        }
    }

    public static class NettyG implements PacketValidation.Unknown, ViolationValidation<WrappedPacket<?>> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPacket<?> packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag(packet.getContainer().getInitialByteWrapped().getAccumulatedByteSize()).build();
            }

            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.G";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.g");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyG.class);
        }
    }

    public static class NettyK implements PacketValidation.Unknown, ViolationValidation<WrappedPacket<?>> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPacket<?> packet, ValidationPipelineContext context) {
            return ValidationServiceContext.PASS;
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.K";
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyK.class);
        }
    }

    public abstract static class AbstractCategorizedCheck implements PacketValidation.Unknown, ViolationValidation<WrappedPacket<?>> {

        private final Map<Long, DecayingListeningCounter> sizeMap = new HashMap<>();
        private long lastFlag;
        private boolean waitingFlag;

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPacket<?> packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                int size = packet.getContainer().getInitialByteWrapped().getByteSize();
                ByteBuf buf = packet.getContainer().getInitialByteWrapped().getBuf().getBuf();

                if (size > getSetting().threshold) {
                    long diff = System.currentTimeMillis() -
                            (lastFlag != 0 ? lastFlag : System.currentTimeMillis() - getSetting().min + 1);
                    lastFlag = System.currentTimeMillis();

                    if (diff > getSetting().min) {
                        boolean foundMatch = false;
                        for (final long sizeKey : new ArrayList<>(sizeMap.keySet())) {
                            if (check(sizeKey, size, buf)) {
                                foundMatch = true;
                                sizeMap.get(sizeKey).count();
                            }
                            if (sizeMap.get(sizeKey).size() == 0) sizeMap.remove(sizeKey);
                        }

                        if (!foundMatch) {
                            final DecayingListeningCounter counter = new DecayingListeningCounter(
                                    () -> (long) getSetting().interval, () -> waitingFlag = true, () -> (long) getSetting().maxPerInterval);
                            counter.count();

                            long key = getKey(size, buf);
                            if (key > 0) sizeMap.put(key, counter);
                        }
                    }
                }
                if (waitingFlag) {
                    waitingFlag = false;
                    return ValidationServiceContext.Builder.validate().cancel().flag(getSetting().maxPerInterval).build();
                }
            }

            return ValidationServiceContext.PASS;
        }

        public abstract boolean check(long sizeKey, int size, ByteBuf byteBuf);

        public abstract long getKey(int size, ByteBuf byteBuf);
    }

    public static class NettyL extends AbstractCategorizedCheck {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.L";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.l");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyL.class);
        }

        @Override
        public boolean check(long sizeKey, int size, ByteBuf buf) {
            return Math.abs(sizeKey - size) < getSetting().max;
        }

        @Override
        public long getKey(int size, ByteBuf buf) {
            return size;
        }
    }

    public abstract static class AbstractFloodDecodeCheck implements PacketValidation.Unknown, ViolationValidation<WrappedPacket<?>> {

        private long lastFlag;

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, WrappedPacket<?> packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()
                    && packet.getContainer().getInitialByteWrapped().getByteSize() > getSetting().threshold) {
                long diff = System.currentTimeMillis() -
                        (lastFlag != 0 ? lastFlag : System.currentTimeMillis() - getSetting().min + 1);

                lastFlag = System.currentTimeMillis();

                if (diff > getSetting().min) {
                    return ValidationServiceContext.Builder.validate().flag().build();
                }
            }

            return ValidationServiceContext.PASS;
        }

        public abstract ConfigurationOptions.ViolationSetting getSetting();
    }

    public static class NettyM extends AbstractFloodDecodeCheck {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.m");
        }

        @Override
        public String getName() {
            return "Netty.M";
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyM.class);
        }
    }

    public static class NettyN extends AbstractFloodDecodeCheck {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.n");
        }

        @Override
        public String getName() {
            return "Netty.N";
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyN.class);
        }
    }

    public static class NettyO extends AbstractCategorizedCheck {

        private static final int id = IDRegistry.CHECKS.acquire();

        public static class LocalCategorizedHandler {

            private long lastReceived = 0;
            private long recentKey;
            private ByteBuf lastByteBuf;
            private boolean begin = true;
            private final Supplier<ConfigurationOptions.ViolationSetting> setting;

            public LocalCategorizedHandler(Supplier<ConfigurationOptions.ViolationSetting> setting) {
                this.setting = setting;
            }

            public boolean check(long sizeKey, int size, ByteBuf buf) {
                if (lastByteBuf == null) lastByteBuf = buf;

                recentKey = getKey(size, buf);

                if (lastReceived == 0 || lastByteBuf != buf) lastReceived = System.currentTimeMillis();
                lastByteBuf = buf;

                return 0 < recentKey && Math.abs(recentKey - sizeKey) < setting.get().max;
            }

            public long getKey(int size, ByteBuf buf) {
                if (begin) {
                    lastReceived = System.currentTimeMillis();
                    begin = false;
                    return 0;
                }

                long key = System.currentTimeMillis() - lastReceived;
                return key > 0 && lastByteBuf != buf ? key : recentKey;
            }
        }

        private final LocalCategorizedHandler categorizedHandler = new LocalCategorizedHandler(this::getSetting);

        @Override
        public int getID() {
            return id;
        }

        @Override
        public String getName() {
            return "Netty.O";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.netty.o");
        }

        @Override
        public ConfigurationOptions.ViolationSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyO.class);
        }

        @Override
        public boolean check(long sizeKey, int size, ByteBuf buf) {
            return categorizedHandler.check(sizeKey, size, buf);
        }

        @Override
        public long getKey(int size, ByteBuf buf) {
            return categorizedHandler.getKey(size, buf);
        }
    }
}
