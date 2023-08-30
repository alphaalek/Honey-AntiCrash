package me.alek.honey.checks.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.DecodeCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.entity.Player;

public class NettyDecodeListener extends AbstractDecodeListener implements IListener {

    @Override
    public CheckVisitor<?> getVisitor() {
        CheckVisitor<?> visitor = new CheckVisitor<>();
        visitor.visit(new NettyD());
        visitor.visit(new NettyE());
        visitor.visit(new NettyF());
        visitor.visit(new NettyG());
        visitor.visit(new NettyH());
        return visitor;
    }

    @Override
    public HoneyCheckType getCheckType() {
        return HoneyCheckType.DECODE;
    }

    public static class NettyD implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {
                // check if the player sends invalid bytes to be decoded to a packet
                if (player.isOnline() && (byteBuf instanceof EmptyByteBuf || byteBuf.readableBytes() == 0)) {

                    NMSUtils.getChannel(player).close();
                    return ResponseBuilder.inst().cancel().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Netty.D";
        }

        @Override
        public String getDescription() {
            return "No bytes to be decoded";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyD.class);
        }
    }

    public static class NettyE implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                int readableBytes = byteBuf.readableBytes();
                return ResponseBuilder.inst().flag(readableBytes).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Netty.E";
        }

        @Override
        public String getDescription() {
            return "Too big packet to be decoded";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyE.class);
        }
    }

    public static class NettyF implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                int readableBytes = byteBuf.readableBytes();
                return ResponseBuilder.inst().flag(readableBytes).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Netty.F";
        }

        @Override
        public String getDescription() {
            return "Buffer of packet bytes exceeded";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyF.class);
        }
    }

    public static class NettyG implements DecodeCheck, ViolationCheck {

        private static final ConstructorInvoker<Object> packetDataSerializerInvoker = Reflection.getConstructor(NMSUtils.getPacketDataSerializerClass(), ByteBuf.class);

        @Override
        public ResponseContainer check(Player player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {

            ByteBuf wrappedByteBuf = Unpooled.wrappedBuffer(byteBuf);
            WrappedPacketDataSerializer dataSerializer = new WrappedPacketDataSerializer(packetDataSerializerInvoker.invoke(wrappedByteBuf), wrappedByteBuf);

            int byteSize = 0;
            while (dataSerializer.isReadable()) {
                byteSize += Math.abs(dataSerializer.readByte());
            }
            container.getStorage().putInt(byteSize);

            if (getSetting().isEnabled()) {
                return ResponseBuilder.inst().flag(byteSize).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Netty.G";
        }

        @Override
        public String getDescription() {
            return "Buffer of absolute bits exceeded";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyG.class);
        }
    }

    public static class NettyH implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {

            if (getSetting().isEnabled()) {
                int byteSize = container.getStorage().getInt(0);

                return ResponseBuilder.inst().flag(byteSize).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Netty.H";
        }

        @Override
        public String getDescription() {
            return "Too high absolute bit amount";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyH.class);
        }
    }
}
