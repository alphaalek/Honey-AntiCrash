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
import java.util.function.Supplier;

public class NettyDecodeListener extends AbstractDecodeListener implements IListener {

    private AbstractCategorizedCheck nettyL;
    private AbstractFloodDecodeCheck nettyM;
    private AbstractFloodDecodeCheck nettyN;
    private AbstractFloodDecodeCheck nettyO;
    private AbstractCategorizedCheck nettyP;

    @Override
    public CheckVisitor<?> getVisitor() {
        CheckVisitor<?> visitor = new CheckVisitor<>();
        // A, B, H, I, J, K are packet listeners and not decode listeners, so they are in a different class
        visitor.visit(new NettyC());
        visitor.visit(new NettyD());
        visitor.visit(new NettyE());
        visitor.visit(new NettyF());
        visitor.visit(new NettyG());
        visitor.visit(nettyL = new NettyL());
        visitor.visit(nettyM = new NettyM());
        visitor.visit(nettyN = new NettyN());
        visitor.visit(nettyO = new NettyO());
        visitor.visit(nettyP = new NettyP());
        return visitor;
    }

    @Override
    public HoneyCheckType getCheckType() {
        return HoneyCheckType.DECODE;
    }

    @Override
    public void cleanup(Player player) {
        nettyL.byteBufSizeMap.remove(player.getUniqueId());
        nettyL.waitingFlagSet.remove(player.getUniqueId());
        nettyL.lastFlagMap.remove(player.getUniqueId());
        nettyP.byteBufSizeMap.remove(player.getUniqueId());
        nettyP.waitingFlagSet.remove(player.getUniqueId());
        nettyP.lastFlagMap.remove(player.getUniqueId());

        nettyM.lastFlagMap.remove(player.getUniqueId());
        nettyN.lastFlagMap.remove(player.getUniqueId());
        nettyO.lastFlagMap.remove(player.getUniqueId());
    }

    public static class DecayableListeningCounter {

        private final Supplier<Long> collectInterval;
        private final Supplier<Long> max;
        private final Runnable onFlag;

        private final LinkedList<Long> data = new LinkedList<>();

        public DecayableListeningCounter(Supplier<Long> collectInterval, Runnable onFlag, Supplier<Long> max) {
            this.collectInterval = collectInterval;
            this.onFlag = onFlag;
            this.max = max;
        }

        public DecayableListeningCounter(Supplier<Long> collectInterval, Runnable onFlag) {
            this(collectInterval, onFlag, null);
        }

        private void collect() {
            synchronized (data) {
                final Iterator<Long> sampleNodeIterator = data.iterator();
                while (sampleNodeIterator.hasNext()) {

                    final long diff = System.currentTimeMillis() - sampleNodeIterator.next();
                    if (diff > collectInterval.get()) {
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
        public String getName() {
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
        public String getName() {
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
        public String getName() {
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
            ByteBuf wrappedByteBuf = Unpooled.copiedBuffer(byteBuf);
            WrappedPacketDataSerializer dataSerializer = new WrappedPacketDataSerializer(packetDataSerializerInvoker.invoke(wrappedByteBuf), wrappedByteBuf);

            int byteSize = 0;
            while (dataSerializer.isReadable()) {
                byteSize += Math.abs(dataSerializer.readByte());
            }
            dataSerializer.release();
            container.getStorage().putInt(byteSize);

            if (getSetting().isEnabled()) {
                return ResponseBuilder.inst().flag(byteSize).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
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
        public String getName() {
            return "Netty.G";
        }

        @Override
        public String getDescription() {
            return "Too high absolute sum of bytes";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyG.class);
        }
    }

    public abstract static class AbstractCategorizedCheck implements DecodeCheck, ViolationCheck {

        private final Map<UUID, Map<Long, DecayableListeningCounter>> byteBufSizeMap = new HashMap<>();
        private final Map<UUID, Long> lastFlagMap = new HashMap<>();
        private final Set<UUID> waitingFlagSet = new HashSet<>();

        @Override
        public ResponseContainer check(Player player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                if (!byteBufSizeMap.containsKey(player.getUniqueId())) {
                    byteBufSizeMap.put(player.getUniqueId(), new HashMap<>());
                }
                final Map<Long, DecayableListeningCounter> byteBufSizeMap = this.byteBufSizeMap.get(player.getUniqueId());

                if (byteBuf.capacity() > getSetting().threshold) {
                    long diff = System.currentTimeMillis() -
                            lastFlagMap.computeIfAbsent(player.getUniqueId(), (d) -> System.currentTimeMillis() - getSetting().min + 1);

                    lastFlagMap.put(player.getUniqueId(), System.currentTimeMillis());
                    if (diff > getSetting().min) {
                        boolean foundMatch = false;
                        for (final long sizeKey : new ArrayList<>(byteBufSizeMap.keySet())) {

                            if (check(player, sizeKey, byteBuf)) {
                                foundMatch = true;
                                byteBufSizeMap.get(sizeKey).count();
                            }
                            if (byteBufSizeMap.get(sizeKey).size() == 0) byteBufSizeMap.remove(sizeKey);
                        }
                        if (!foundMatch) {
                            final DecayableListeningCounter counter = new DecayableListeningCounter(
                                    () -> (long) getSetting().interval, () -> waitingFlagSet.add(player.getUniqueId()), () -> (long) getSetting().maxPerInterval);
                            counter.count();

                            long key = getKey(player, byteBuf);
                            if (key > 0) byteBufSizeMap.put(key, counter);
                        }
                    }
                }
                if (waitingFlagSet.contains(player.getUniqueId())) {
                    waitingFlagSet.remove(player.getUniqueId());
                    return ResponseBuilder.inst().cancel().flag(getSetting().maxPerInterval).build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        public abstract boolean check(Player player, long sizeKey, ByteBuf byteBuf);

        public abstract long getKey(Player player, ByteBuf byteBuf);
    }

    public static class NettyL extends AbstractCategorizedCheck {

        @Override
        public String getName() {
            return "Netty.L";
        }

        @Override
        public String getDescription() {
            return "Too many categorized big packets";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyL.class);
        }

        @Override
        public boolean check(Player player, long sizeKey, ByteBuf byteBuf) {
            return Math.abs(sizeKey - byteBuf.capacity()) < getSetting().max;
        }

        @Override
        public long getKey(Player player, ByteBuf byteBuf) {
            return byteBuf.capacity();
        }
    }

    public abstract static class AbstractFloodDecodeCheck implements DecodeCheck, ViolationCheck {

        private final Map<UUID, Long> lastFlagMap = new HashMap<>();

        @Override
        public ResponseContainer check(Player player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled() && byteBuf.capacity() > getSetting().threshold) {
                long diff = System.currentTimeMillis() -
                        lastFlagMap.computeIfAbsent(player.getUniqueId(), (d) -> System.currentTimeMillis() - getSetting().min + 1);

                lastFlagMap.put(player.getUniqueId(), System.currentTimeMillis());

                if (diff > getSetting().min) {
                    return ResponseBuilder.inst().flag().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getDescription() {
            return "Packet flood for big packets to be decoded";
        }

        public abstract ConfigurationOptions.VlSetting getSetting();

    }

    public static class NettyM extends NettyDecodeListener.AbstractFloodDecodeCheck {

        @Override
        public String getName() {
            return "Netty.M";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyM.class);
        }
    }

    public static class NettyN extends AbstractFloodDecodeCheck {

        @Override
        public String getName() {
            return "Netty.N";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyN.class);
        }
    }

    public static class NettyO extends AbstractFloodDecodeCheck {

        @Override
        public String getName() {
            return "Netty.O";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyO.class);
        }
    }

    public static class NettyP extends AbstractCategorizedCheck {

        private long lastReceived = 0;
        private long recentKey;
        private ByteBuf lastByteBuf;
        private boolean begin = true;

        @Override
        public String getName() {
            return "Netty.P";
        }

        @Override
        public String getDescription() {
            return "Too many categorized timed big packets";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyP.class);
        }

        @Override
        public boolean check(Player player, long sizeKey, ByteBuf byteBuf) {
            if (lastByteBuf == null) lastByteBuf = byteBuf;

            recentKey = getKey(player, byteBuf);

            if (lastReceived == 0 || lastByteBuf != byteBuf) lastReceived = System.currentTimeMillis();
            lastByteBuf = byteBuf;

            return 0 < recentKey && Math.abs(recentKey - sizeKey) < getSetting().max;
        }

        @Override
        public long getKey(Player player, ByteBuf byteBuf) {
            if (begin) {
                lastReceived = System.currentTimeMillis();
                begin = false;
                return 0;
            }

            long key = System.currentTimeMillis() - lastReceived;
            return key > 0 && lastByteBuf != byteBuf ? key : recentKey;
        }
    }
}
