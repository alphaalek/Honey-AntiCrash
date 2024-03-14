package me.alek.honey.checks.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.DecodeCheck;
import me.alek.honey.checks.types.ViolationCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilterFactory;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.services.ViolationService;
import me.alek.honey.tracker.temp.WideTempQueuedHandleTracker;
import me.alek.honey.tracker.timed.Timespan;
import me.alek.honey.utility.DecayingListeningCounter;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.function.Supplier;

public class NettyDecodeListener extends AbstractDecodeListener implements IListener {

    @Override
    public CheckVisitor<?> getVisitor() {
        CheckVisitor<?> visitor = new CheckVisitor<>();
        // A, B, H, I, J, K are packet listeners and not decode listeners, so they are in a different class
        visitor.visit(new NettyC());
        visitor.visit(new NettyD());
        visitor.visit(new NettyE());
        visitor.visit(new NettyF());
        visitor.visit(new NettyG());
        visitor.visit(new NettyK());
        visitor.visit(new NettyL());
        visitor.visit(new NettyM());
        visitor.visit(new NettyN());
        visitor.visit(new NettyO());
        return visitor;
    }

    @Override
    public HoneyCheckType getCheckType() {
        return HoneyCheckType.DECODE;
    }

    public static class NettyC implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {
                // check if the player sends invalid bytes to be decoded to a packet
                if (byteBuf instanceof EmptyByteBuf || byteBuf.readableBytes() == 0) {

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
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilterFactory.createEmptyFilter();
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyC.class);
        }
    }

    public static class NettyD implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
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
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilterFactory.createEmptyFilter();
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyD.class);
        }
    }

    public static class NettyE implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
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
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilterFactory.createEmptyFilter();
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyE.class);
        }
    }

    public static class NettyF implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            int prevReaderIndex = byteBuf.readerIndex();

            int byteSize = 0;
            while (byteBuf.isReadable()) {
                byteSize += Math.abs(byteBuf.readByte());
            }
            byteBuf.readerIndex(prevReaderIndex);

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
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilterFactory.createEmptyFilter();
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyF.class);
        }
    }

    public static class NettyG implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
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
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilterFactory.createEmptyFilter();
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyG.class);
        }
    }

    public static class NettyK implements DecodeCheck, ViolationCheck {

        private static final WideTempQueuedHandleTracker tracker = HoneyPlugin.get().getNettyPacketHandleTracker();
        private static final ResponseContainer success = ResponseBuilder.inst().add(
                ResponseBuilder.inst().flag(0).withViolationKey(new ViolationService.CheckKey("ta")).build(),
                ResponseBuilder.inst().flag(0).withViolationKey(new ViolationService.CheckKey("tt")).build()
        ).build();

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                double ta = tracker.getTimeToAverageWithoutQuotient(player, Timespan.TEN_SECOND, false);
                if (ta > 5) {
                    int size = tracker.getTrackerSize();
                    if ((size > 15 && ta > 17.5) || (size > 25 && ta > 15) || (size > 40 && ta > 12.5)) {
                        return ResponseBuilder.inst().cancel().withViolationKey(new ViolationService.CheckKey("ta")).flag(1).build();
                    }

                    double tt = tracker.getTimeToTotalQuotient(player, Timespan.TEN_SECOND, false);
                    if ((size > 15 && tt > 0.65) || (size > 25 && tt > 0.55)) {
                        return ResponseBuilder.inst().cancel().withViolationKey(new ViolationService.CheckKey("tt")).flag(1).build();
                    }
                }
                return success;
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Netty.K";
        }

        @Override
        public String getDescription() {
            return "Too high usage of netty threads";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LENIENT_LONG_FILTER;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyK.class);
        }
    }

    public abstract static class AbstractCategorizedCheck implements DecodeCheck, ViolationCheck {

        private final Map<Long, DecayingListeningCounter> sizeMap = new HashMap<>();
        private long lastFlag;
        private boolean waitingFlag;

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                if (byteBuf.capacity() > getSetting().threshold) {

                    long diff = System.currentTimeMillis() -
                            (lastFlag != 0 ? lastFlag : System.currentTimeMillis() - getSetting().min + 1);

                    lastFlag = System.currentTimeMillis();
                    if (diff > getSetting().min) {

                        boolean foundMatch = false;
                        for (final long sizeKey : new ArrayList<>(sizeMap.keySet())) {
                            if (check(player, sizeKey, byteBuf)) {
                                foundMatch = true;
                                sizeMap.get(sizeKey).count();
                            }
                            if (sizeMap.get(sizeKey).size() == 0) sizeMap.remove(sizeKey);
                        }
                        if (!foundMatch) {
                            final DecayingListeningCounter counter = new DecayingListeningCounter(
                                    () -> (long) getSetting().interval, () -> waitingFlag = true, () -> (long) getSetting().maxPerInterval);
                            counter.count();

                            long key = getKey(player, byteBuf);
                            if (key > 0) sizeMap.put(key, counter);
                        }
                    }
                }
                if (waitingFlag) {
                    waitingFlag = false;
                    return ResponseBuilder.inst().cancel().flag(getSetting().maxPerInterval).build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LENIENT_LONG_FILTER;
        }

        public abstract boolean check(HoneyPlayer player, long sizeKey, ByteBuf byteBuf);

        public abstract long getKey(HoneyPlayer player, ByteBuf byteBuf);
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
        public boolean check(HoneyPlayer player, long sizeKey, ByteBuf byteBuf) {
            return Math.abs(sizeKey - byteBuf.capacity()) < getSetting().max;
        }

        @Override
        public long getKey(HoneyPlayer player, ByteBuf byteBuf) {
            return byteBuf.capacity();
        }
    }

    public abstract static class AbstractFloodDecodeCheck implements DecodeCheck, ViolationCheck {

        private long lastFlag;

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled() && byteBuf.capacity() > getSetting().threshold) {
                long diff = System.currentTimeMillis() -
                        (lastFlag != 0 ? lastFlag : System.currentTimeMillis() - getSetting().min + 1);

                lastFlag = System.currentTimeMillis();

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

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LENIENT_LONG_FILTER;
        }

        public abstract ConfigurationOptions.VlSetting getSetting();

    }

    public static class NettyM extends AbstractFloodDecodeCheck {

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

    public static class NettyO extends AbstractCategorizedCheck {

        public static class LocalCategorizedHandler {

            private long lastReceived = 0;
            private long recentKey;
            private ByteBuf lastByteBuf;
            private boolean begin = true;
            private final Supplier<ConfigurationOptions.VlSetting> setting;

            public LocalCategorizedHandler(Supplier<ConfigurationOptions.VlSetting> setting) {
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
        public String getName() {
            return "Netty.O";
        }

        @Override
        public String getDescription() {
            return "Too many timed categorized big packets";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyO.class);
        }

        @Override
        public boolean check(HoneyPlayer player, long sizeKey, ByteBuf byteBuf) {
            return categorizedHandler.check(sizeKey, byteBuf);
        }

        @Override
        public long getKey(HoneyPlayer player, ByteBuf byteBuf) {
            return categorizedHandler.getKey(byteBuf);
        }
    }
}
