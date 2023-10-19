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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NettyDecodeListener extends AbstractDecodeListener implements IListener {

    @Override
    public CheckVisitor<?> getVisitor() {
        CheckVisitor<?> visitor = new CheckVisitor<>();
        // A, B, H, I, J, K are packet listeners
        visitor.visit(new NettyC());
        visitor.visit(new NettyD());
        visitor.visit(new NettyE());
        visitor.visit(new NettyF());
        visitor.visit(new NettyG());
        visitor.visit(new NettyL());
        return visitor;
    }

    @Override
    public HoneyCheckType getCheckType() {
        return HoneyCheckType.DECODE;
    }

    @Override
    public void cleanup(Player player) {
        NettyL.byteBufSizeMap.remove(player.getUniqueId());
    }

    public static class DecayableListeningCounter {

        private final long collectInterval;
        private final Supplier<Long> max;
        private final Runnable onFlag;

        private final LinkedList<Long> data = new LinkedList<>();

        public DecayableListeningCounter(long collectInterval, Runnable onFlag, Supplier<Long> max) {
            this.collectInterval = collectInterval;
            this.onFlag = onFlag;
            this.max = max;
        }

        public DecayableListeningCounter(long collectInterval, Runnable onFlag) {
            this(collectInterval, onFlag, null);
        }

        private void collect() {
            synchronized (data) {
                final Iterator<Long> sampleNodeIterator = data.iterator();
                while (sampleNodeIterator.hasNext()) {

                    final long diff = System.currentTimeMillis() - sampleNodeIterator.next();
                    if (diff > collectInterval) {
                        sampleNodeIterator.remove();
                        continue;
                    }
                    break;
                }
            }
        }

        public void count() {
            collect();

            synchronized (data) {
                data.addLast(System.currentTimeMillis());
            }
            if (max != null && data.size() > max.get()) {
                onFlag.run();

                synchronized (data) {
                    data.clear();
                }
            }
        }

        public int size() {
            collect();

            return data.size();
        }
    }

    public static class NettyC implements DecodeCheck, ViolationCheck {

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
        public String getCheckName() {
            return "Netty.C";
        }

        @Override
        public String getDescription() {
            return "No bytes to be decoded";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyC.class);
        }
    }

    public static class NettyD implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                int readableBytes = byteBuf.readableBytes();
                return ResponseBuilder.inst().flag(readableBytes).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Netty.D";
        }

        @Override
        public String getDescription() {
            return "Too big packet to be decoded";
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
        public String getCheckName() {
            return "Netty.E";
        }

        @Override
        public String getDescription() {
            return "Buffer of packet bytes exceeded";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyE.class);
        }
    }

    public static class NettyF implements DecodeCheck, ViolationCheck {

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
        public String getCheckName() {
            return "Netty.F";
        }

        @Override
        public String getDescription() {
            return "Buffer of absolute bits exceeded";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyF.class);
        }
    }

    public static class NettyG implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(Player player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {
                int byteSize = container.getStorage().getInt(0);

                return ResponseBuilder.inst().flag(byteSize).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Netty.G";
        }

        @Override
        public String getDescription() {
            return "Too high absolute bit amount";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyG.class);
        }
    }

    public static class NettyL implements DecodeCheck, ViolationCheck {

        private static final Map<UUID, Map<Integer, DecayableListeningCounter>> byteBufSizeMap = new HashMap<>();
        private static final Set<UUID> waitingFlagSet = new HashSet<>();

        @Override
        public ResponseContainer check(Player player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                if (!byteBufSizeMap.containsKey(player.getUniqueId())) {
                    byteBufSizeMap.put(player.getUniqueId(), new HashMap<>());
                }
                final Map<Integer, DecayableListeningCounter> byteBufSizeMap = NettyL.byteBufSizeMap.get(player.getUniqueId());

                if (byteBuf.capacity() > getSetting().threshold) {
                    boolean foundMatch = false;
                    for (final int sizeKey : byteBufSizeMap.keySet()) {

                        if (Math.abs(sizeKey - byteBuf.capacity()) < 200) {
                            foundMatch = true;
                            byteBufSizeMap.get(sizeKey).count();
                        } else break;
                    }
                    if (!foundMatch) {
                        final DecayableListeningCounter counter = new DecayableListeningCounter(
                                getSetting().interval, () -> waitingFlagSet.add(player.getUniqueId()), () -> (long) getSetting().maxPerInterval);
                        counter.count();
                        byteBufSizeMap.put(byteBuf.capacity(), counter);
                    }
                }
                if (waitingFlagSet.contains(player.getUniqueId())) {
                    waitingFlagSet.remove(player.getUniqueId());
                    return ResponseBuilder.inst().cancel().flag(getSetting().maxPerInterval + 1).build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Netty.L";
        }

        @Override
        public String getDescription() {
            return "Too many categorized big packets in interval";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyL.class);
        }
    }
}
