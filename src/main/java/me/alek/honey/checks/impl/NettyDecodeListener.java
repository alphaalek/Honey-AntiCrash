package me.alek.honey.checks.impl;

import io.netty.buffer.ByteBuf;
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
import me.alek.honey.util.DecayingListeningCounter;

import java.util.*;
import java.util.function.Supplier;

public class NettyDecodeListener extends AbstractDecodeListener implements IListener {

    @Override
    public CheckVisitor<?> getVisitor(HoneyPlayer player) {
        CheckVisitor<?> visitor = new CheckVisitor<>(player);
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

    private static void processByteBuf(ByteBuf byteBuf, CheckContextContainer container) {
        if (byteBuf.readableBytes() == 0) {
            container.getStorage().putInt(0);
            return;
        }

        int accumulated = 0;

        int readerIndex = byteBuf.readerIndex();
        do {
            byte currentByte = byteBuf.readByte();
            accumulated += Math.abs(currentByte) + 8;

        } while (byteBuf.isReadable());

        byteBuf.readerIndex(readerIndex);

        container.getStorage().putInt(accumulated);
    }

    @Override
    public HoneyCheckType getCheckType() {
        return HoneyCheckType.DECODE;
    }

    public static class NettyD implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            processByteBuf(byteBuf, container);

            int size = byteBuf.readableBytes();
            container.getStorage().putInt(size);

            if (getSetting().isEnabled()) {
                return ResponseBuilder.inst().flag(size).build();
            }
            return ResponseBuilder.PASS;
        }

        @Override
        public int getID() {
            return 29;
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
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyD.class);
        }
    }

    public static class NettyE implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {
                return ResponseBuilder.inst().flag(container.getStorage().getInt(1)).build();
            }
            return ResponseBuilder.PASS;
        }

        @Override
        public int getID() {
            return 30;
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
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyE.class);
        }
    }

    public static class NettyF implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {
                return ResponseBuilder.inst().flag(container.getStorage().getInt(0)).build();
            }
            return ResponseBuilder.PASS;
        }

        @Override
        public int getID() {
            return 31;
        }

        @Override
        public String getName() {
            return "Netty.F";
        }

        @Override
        public String getDescription() {
            return "Buffer of accumulated byte count exceeded";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilterFactory.createEmptyFilter();
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyF.class);
        }
    }

    public static class NettyG implements DecodeCheck, ViolationCheck {

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {
                return ResponseBuilder.inst().flag(container.getStorage().getInt(0)).build();
            }
            return ResponseBuilder.PASS;
        }

        @Override
        public int getID() {
            return 32;
        }

        @Override
        public String getName() {
            return "Netty.G";
        }

        @Override
        public String getDescription() {
            return "Too high accumulated byte count";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilterFactory.createEmptyFilter();
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyG.class);
        }
    }

    public static class NettyK implements DecodeCheck, ViolationCheck {

        private static final WideTempQueuedHandleTracker tracker = HoneyPlugin.get().getNettyPacketTracker();
        private static final double MIN_TA_ENTER = 5;

        private final ViolationService.HoneyKey TAKey = ViolationService.HoneyKey.of(0, "ta");
        private final ViolationService.HoneyKey TTKey = ViolationService.HoneyKey.of(1, "tt");
        private final ResponseContainer success = ResponseBuilder.inst().add(
                ResponseBuilder.inst().flag(0).withViolationKey(TAKey).build(),
                ResponseBuilder.inst().flag(0).withViolationKey(TTKey).build()
        ).build();

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf packet, CheckContextContainer container) throws Exception {
            /*
            if (getSetting().isEnabled() && player != null) {

                double ta = tracker.getTimeToAverageWithoutQuotient(player, Timespan.TEN_SECOND, false);
                if (ta > MIN_TA_ENTER) {
                    int size = tracker.getTrackerSize();
                    if ((size > 15 && ta > 17.5) || (size > 25 && ta > 15) || (size > 40 && ta > 12.5)) {
                        return ResponseBuilder.inst().cancel().withViolationKey(TAKey).flag(1).build();
                    }

                    double tt = tracker.getTimeToTotalQuotient(player, Timespan.TEN_SECOND, false);
                    if ((size > 15 && tt > 0.65) || (size > 25 && tt > 0.55)) {
                        return ResponseBuilder.inst().cancel().withViolationKey(TTKey).flag(1).build();
                    }
                }
                return success;
            }
            */
            return ResponseBuilder.PASS;
        }

        @Override
        public int getID() {
            return 36;
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
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyK.class);
        }
    }

    public abstract static class AbstractCategorizedCheck implements DecodeCheck, ViolationCheck {

        private final Map<Long, DecayingListeningCounter> sizeMap = new HashMap<>();
        private long lastFlag;
        private boolean waitingFlag;

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
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
                    return ResponseBuilder.inst().cancel().flag(getSetting().maxPerInterval).build();
                }
            }
            return ResponseBuilder.PASS;
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LENIENT_LONG_FILTER;
        }

        public abstract boolean check(long sizeKey, ByteBuf byteBuf);

        public abstract long getKey(ByteBuf byteBuf);
    }

    public static class NettyL extends AbstractCategorizedCheck {

        @Override
        public int getID() {
            return 37;
        }

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

    public abstract static class AbstractFloodDecodeCheck implements DecodeCheck, ViolationCheck {

        private long lastFlag;

        @Override
        public ResponseContainer check(HoneyPlayer player, ByteBuf byteBuf, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled() && byteBuf.readableBytes() > getSetting().threshold) {
                long diff = System.currentTimeMillis() -
                        (lastFlag != 0 ? lastFlag : System.currentTimeMillis() - getSetting().min + 1);

                lastFlag = System.currentTimeMillis();

                if (diff > getSetting().min) {
                    return ResponseBuilder.inst().flag().build();
                }
            }
            return ResponseBuilder.PASS;
        }

        @Override
        public String getDescription() {
            return "Packet flood for big packets";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.LENIENT_LONG_FILTER;
        }

        public abstract ConfigurationOptions.VlSetting getSetting();

    }

    public static class NettyM extends AbstractFloodDecodeCheck {

        @Override
        public int getID() {
            return 38;
        }

        @Override
        public String getName() {
            return "Netty.M";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyM.class);
        }
    }

    public static class NettyN extends AbstractFloodDecodeCheck {

        @Override
        public int getID() {
            return 39;
        }

        @Override
        public String getName() {
            return "Netty.N";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return Settings.getSetting(this, ConfigurationOptions.Netty.NettyN.class);
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
        public int getID() {
            return 40;
        }

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
