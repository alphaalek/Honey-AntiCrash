package me.alek.honey.validation.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.util.IDRegistry;
import me.alek.honey.validation.*;

public class NettyDecodeValidations {

    public static final ValidationPipeline<PacketValidation.Byte> BYTE_FILTER_PIPELINE = ValidationPipeline.from(new NettyD(), new NettyE(), new NettyF(), new NettyG());

    public static class NettyD implements PacketValidation.Byte, ViolationValidation<ByteInWrappedPacket<?>> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, ByteInWrappedPacket<?> packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationViolationContext.Builder.validate().flag(packet.bytes()).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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

    public static class NettyE implements PacketValidation.Byte, ViolationValidation<ByteInWrappedPacket<?>> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, ByteInWrappedPacket<?> packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationViolationContext.Builder.validate().flag(packet.bytes()).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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

    public static class NettyF implements PacketValidation.Byte, ViolationValidation<ByteInWrappedPacket<?>> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, ByteInWrappedPacket<?> packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationViolationContext.Builder.validate().flag(packet.accumulatedBytes()).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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

    public static class NettyG implements PacketValidation.Byte, ViolationValidation<ByteInWrappedPacket<?>> {

        private static final int id = IDRegistry.VALIDATION.acquire();

        @Override
        public ValidationViolationContext validate(HoneyPlayer player, ByteInWrappedPacket<?> packet, ValidationPipelineContext context) {
            if (getSetting().isEnabled()) {
                return ValidationViolationContext.Builder.validate().flag(packet.accumulatedBytes()).build();
            }

            return ValidationViolationContext.PASS;
        }

        @Override
        public Integer getID() {
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
}
