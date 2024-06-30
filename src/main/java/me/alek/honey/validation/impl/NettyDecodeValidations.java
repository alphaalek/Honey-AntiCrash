package me.alek.honey.validation.impl;

import io.netty.buffer.ByteBuf;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.validation.*;
import me.alek.honey.validation.ValidationServiceContext;
import me.alek.honey.validation.types.DecodeValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.util.counter.DecayingListeningCounter;
import me.alek.honey.util.IDRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class NettyDecodeValidations<WP extends WrappedPacket<WP>> extends AbstractDecodeListener<WP> {

    @Override
    public ValidationPipeline<DecodeValidation> getNewPipeline() {
        return ValidationPipeline.from(new NettyD(), new NettyE(), new NettyF(), new NettyG(), new NettyK(), new NettyL(), new NettyM(), new NettyN(), new NettyO());
    }

    private static void processByteBuf(ByteBuf byteBuf, ValidationPipelineContext context) {
        if (!byteBuf.isReadable()) {
            return;
        }

        int accumulated = 0;
        do {
            byte currentByte = byteBuf.readByte();
            accumulated += Math.abs(currentByte) + 8;
        } while (byteBuf.isReadable());
        context.putInt(accumulated);

        byteBuf.resetReaderIndex();
    }

    public static class NettyD implements DecodeValidation, ViolationValidation<ByteBuf> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, ByteBuf byteBuf, ValidationPipelineContext context) {
            processByteBuf(byteBuf, context);

            int size = byteBuf.readableBytes();
            context.putInt(size);

            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag(size).build();
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

    public static class NettyE implements DecodeValidation, ViolationValidation<ByteBuf> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, ByteBuf byteBuf, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag(context.getInt(1)).build();
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

    public static class NettyF implements DecodeValidation, ViolationValidation<ByteBuf> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, ByteBuf byteBuf, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag(context.getInt(0)).build();
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

    public static class NettyG implements DecodeValidation, ViolationValidation<ByteBuf> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, ByteBuf byteBuf, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationServiceContext.Builder.validate().flag(context.getInt(0)).build();
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

    public static class NettyK implements DecodeValidation, ViolationValidation<ByteBuf> {

        private static final int id = IDRegistry.CHECKS.acquire();

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, ByteBuf packet, ValidationPipelineContext context) {
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

    public abstract static class AbstractCategorizedCheck implements DecodeValidation, ViolationValidation<ByteBuf> {

        private final Map<Long, DecayingListeningCounter> sizeMap = new HashMap<>();
        private long lastFlag;
        private boolean waitingFlag;

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, ByteBuf byteBuf, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {

                if (byteBuf.readableBytes() > getSetting().threshold) {
                    long diff = System.currentTimeMillis() -
                            (lastFlag != 0 ? lastFlag : System.currentTimeMillis() - getSetting().min + 1);
                    lastFlag = System.currentTimeMillis();

                    if (diff > getSetting().min) {
                        boolean foundMatch = false;
                        for (final long sizeKey : new ArrayList<>(sizeMap.keySet())) {
                            if (check(sizeKey, byteBuf)) {
                                foundMatch = true;
                                sizeMap.get(sizeKey).count();
                            }
                            if (sizeMap.get(sizeKey).size() == 0) sizeMap.remove(sizeKey);
                        }

                        if (!foundMatch) {
                            final DecayingListeningCounter counter = new DecayingListeningCounter(
                                    () -> (long) getSetting().interval, () -> waitingFlag = true, () -> (long) getSetting().maxPerInterval);
                            counter.count();

                            long key = getKey(byteBuf);
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

        public abstract boolean check(long sizeKey, ByteBuf byteBuf);

        public abstract long getKey(ByteBuf byteBuf);
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
        public boolean check(long sizeKey, ByteBuf byteBuf) {
            return Math.abs(sizeKey - byteBuf.readableBytes()) < getSetting().max;
        }

        @Override
        public long getKey(ByteBuf byteBuf) {
            return byteBuf.readableBytes();
        }
    }

    public abstract static class AbstractFloodDecodeCheck implements DecodeValidation, ViolationValidation<ByteBuf> {

        private long lastFlag;

        @Override
        public ValidationServiceContext validate(HoneyPlayer player, ByteBuf byteBuf, ValidationPipelineContext context) {
            if (getSetting().isEnabled()
                    && byteBuf.readableBytes() > getSetting().threshold) {
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

            public boolean check(long sizeKey, ByteBuf byteBuf) {
                if (lastByteBuf == null) lastByteBuf = byteBuf;

                recentKey = getKey(byteBuf);

                if (lastReceived == 0 || lastByteBuf != byteBuf) lastReceived = System.currentTimeMillis();
                lastByteBuf = byteBuf;

                return 0 < recentKey && Math.abs(recentKey - sizeKey) < setting.get().max;
            }

            public long getKey(ByteBuf byteBuf) {
                if (begin) {
                    lastReceived = System.currentTimeMillis();
                    begin = false;
                    return 0;
                }

                long key = System.currentTimeMillis() - lastReceived;
                return key > 0 && lastByteBuf != byteBuf ? key : recentKey;
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
        public boolean check(long sizeKey, ByteBuf byteBuf) {
            return categorizedHandler.check(sizeKey, byteBuf);
        }

        @Override
        public long getKey(ByteBuf byteBuf) {
            return categorizedHandler.getKey(byteBuf);
        }
    }
}
