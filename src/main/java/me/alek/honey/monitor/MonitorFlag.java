package me.alek.honey.monitor;

import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.PacketWrapperInvokerFactory;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.tracker.timed.Timespan;
import me.alek.honey.utility.PredicateUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public interface MonitorFlag<T> {

    enum Flag {
        PLAYER("player", "--player [player name]", false, PlayerFlag::new),
        TYPE("type", "--type [packet type]", false, EqualTypeFlag::new),
        IGNORE("ignore", "--ignore [packet type]", false, IgnoreTypeFlag::new),
        IGNORE_POSITION("ignore-position", "--ignore-position", false, IgnorePositionFlag::new),
        CANCELLED("cancelled", "--cancelled", false, CancelledFlag::new),
        HONEY_DECODE("min-honey-decode", "--min-honey-decode [number (as decimal)]", true, HoneyDecodeTimingsFlag::new),
        SERVER_DECODE("min-server-decode", "--min-server-decode [number (as decimal)]", true, ServerDecodeTimingsFlag::new),
        HONEY_HANDLE("min-honey-handle", "--min-honey-handle [number (as decimal)]", true, HoneyHandleTimingsFlag::new),
        SERVER_HANDLE("min-server-handle", "--min-server-handle [number (as decimal)]", true, ServerHandleTimingsFlag::new),
        NTQ_OAWO("min-ntq-oawo", "--min-ntq-oawo [number (as percentage)]", true, NTQOfAverageWithoutFlag::new),
        NTQ_OT("min-ntq-ot", "--min-ntq-ot [number (as percentage)]", true, NTQOfTotalFlag::new),
        STQ_OAWO("min-stq-oawo", "--min-stq-oawo [number (as percentage)]", true, STQOfAverageWithoutFlag::new),
        STQ_OT("min-stq-ot", "--min-stq-ot [number (as percentage)]", true, STQOfTotalFlag::new),
        STQ_OHST("min-ohst", "--min-ohst [number (as percentage)]", true, STQOfHalfServerThreadFlag::new),
        PERFORMANCE("min-performance", "--min-performance [number (as percentage)]", true, PerformanceFlag::new),
        ;

        private final String name;
        private final String usage;
        private final boolean hasToBeAfterServerHandle;
        private final Supplier<MonitorFlag<?>> monitorFlagSupplier;

        Flag(String name, String usage, boolean hasToBeAfterServerHandle, Supplier<MonitorFlag<?>> monitorFlagSupplier) {
            this.name = name;
            this.usage = usage;
            this.hasToBeAfterServerHandle = hasToBeAfterServerHandle;
            this.monitorFlagSupplier = monitorFlagSupplier;
        }

        public String getName() {
            return this.name;
        }

        public String getUsage() {
            return this.usage;
        }

        public boolean hasToBeAfterServerHandle() {
            return hasToBeAfterServerHandle;
        }

        public MonitorFlag<?> createNew() {
            return monitorFlagSupplier.get();
        }

        private static final Map<String, Flag> flagsLookup = new HashMap<>();

        static {
            for (Flag flag : Flag.values()) {
                flagsLookup.put(flag.getName(), flag);
            }
        }

        public static Flag getFlagByName(String name) {
            return flagsLookup.get(name);
        }

        public static void checkSyntax(String[] args, int readIndex, MonitorFlag<?> flag, int takes) throws WrongMonitorFlagSyntaxException {
            if (readIndex - 1 + takes >= args.length) {
                throw new WrongMonitorFlagSyntaxException(flag);
            }
        }

        public static Flag getFlag(MonitorFlag<?> flag) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends MonitorFlag<?>> clazz = (Class<? extends MonitorFlag<?>>) flag.getClass();

                return clazz.getAnnotation(FlagAnnotation.class).value();
            } catch (Exception ex) {
                return null;
            }
        }

        public static List<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> getMonitorFlags(String[] args) throws WrongMonitorFlagSyntaxException {
            if (args.length == 0) return new ArrayList<>();

            List<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> flags = new ArrayList<>();
            int readIndex = 0;

            while (readIndex < args.length) {
                String argument = args[readIndex++];
                if (argument.startsWith("--")) {

                    MonitorFlag.Flag flagType = MonitorFlag.Flag.getFlagByName(argument.toLowerCase().substring(2));
                    if (flagType == null) continue;

                    @SuppressWarnings("unchecked")
                    MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag =
                            (MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>) flagType.createNew();
                    readIndex += flag.check(args, readIndex);
                    flags.add(flag);
                }
            }
            return flags;
        }
    }

    interface NoArgumentMonitorFlag<F> extends MonitorFlag<F> {

        @Override
        default int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return 0;
        }
    }

    interface NoAppendingMonitorFlag<F> extends MonitorFlag<F> {

        @Override
        default void append(F input, List<String> data) {}
    }

    int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException;

    boolean test(T input);

    void append(T input, List<String> data);

    default String getWrongSyntaxMessage() {
        return "You have to choose an argument";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface FlagAnnotation {
        Flag value();
    }

    class NumberArgumentFlag implements NoAppendingMonitorFlag<Double> {

        private double value;
        private final MonitorFlag<?> flag;

        public NumberArgumentFlag(MonitorFlag<?> flag) {
            this.flag = flag;
        }

        protected double getValue() {
            return value;
        }

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, readIndex, flag, 1);

            try {
                this.value = Double.parseDouble(args[readIndex]);
            } catch (NumberFormatException ex) {
                throw new WrongMonitorFlagSyntaxException(flag);
            }
            return 1;
        }

        @Override
        public String getWrongSyntaxMessage() {
            return "You have to type a valid number.";
        }

        @Override
        public boolean test(Double input) {
            return input > value;
        }
    }

    @FlagAnnotation(Flag.PLAYER)
    class PlayerFlag implements NoAppendingMonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private String playerName;

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, readIndex, this, 1);

            this.playerName = args[readIndex];
            return 1;
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return input.getPlayer().name.equalsIgnoreCase(playerName);
        }
    }

    class TypeFlag implements NoAppendingMonitorFlag<PacketTypeEnum> {

        private String type;
        private final BiPredicate<String, String> typeFlagComparator;
        private final MonitorFlag<?> flag;

        public TypeFlag(MonitorFlag<?> flag, BiPredicate<String, String> predicate) {
            this.flag = flag;
            this.typeFlagComparator = predicate;
        }

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, readIndex, flag, 1);

            this.type = args[readIndex];
            {
                PacketTypeEnum packetType = PacketWrapperInvokerFactory.getPacketTypeForNameIgnoreCase(type);
                if (packetType != null) {
                    this.type = packetType.toString();
                    return 1;
                }
            }
            for (PacketTypeEnum packetType : PacketType.getTypesAvailableFor(NetProtocol.getProtocol(), PacketState.PLAY, PacketBound.CLIENT)) {
                if (packetType.toString().equalsIgnoreCase(type)) {
                    return 1;
                }
            }
            throw new WrongMonitorFlagSyntaxException(flag);
        }

        @Override
        public boolean test(PacketTypeEnum input) {
            return typeFlagComparator.test(type, input.toString());
        }

        @Override
        public String getWrongSyntaxMessage() {
            return "Couldn't find any packet type matching \"" + Optional.ofNullable(type).orElse("") + "\"";
        }
    }

    @FlagAnnotation(Flag.TYPE)
    class EqualTypeFlag implements NoAppendingMonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<PacketTypeEnum> typeFlag = new TypeFlag(this, PredicateUtils.STRING_EQUAL_COMPARATOR);

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return typeFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return typeFlag.test(input.getType());
        }

        @Override
        public String getWrongSyntaxMessage() {
            return typeFlag.getWrongSyntaxMessage();
        }
    }

    @FlagAnnotation(Flag.IGNORE)
    class IgnoreTypeFlag implements NoAppendingMonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<PacketTypeEnum> typeFlag = new TypeFlag(this, PredicateUtils.STRING_NOT_EQUAL_COMPARATOR);

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return typeFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return typeFlag.test(input.getType());
        }

        @Override
        public String getWrongSyntaxMessage() {
            return typeFlag.getWrongSyntaxMessage();
        }
    }

    @FlagAnnotation(Flag.IGNORE_POSITION)
    class IgnorePositionFlag implements NoAppendingMonitorFlag<PacketContainer<? extends WrappedPacket<?>>>, NoArgumentMonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private static final PacketTypeEnum[] POSITION_PACKETS = Arrays.asList(
                PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.LOOK, PacketType.Play.Client.FLYING).toArray(new PacketTypeEnum[0]);

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            for (PacketTypeEnum packetType : POSITION_PACKETS) {
                if (input.getType().equals(packetType)) return false;
            }
            return true;
        }
    }

    @FlagAnnotation(Flag.CANCELLED)
    class CancelledFlag implements NoAppendingMonitorFlag<PacketContainer<? extends WrappedPacket<?>>>, NoArgumentMonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return input.isCancelled();
        }
    }

    abstract class TimingsFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(input.getEventloopStateTimings().getTimeForMillis(getState()));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, List<String> data) {
            data.add(getState().getFormat() + ":" + input.getEventloopStateTimings().getTimeForMillisFormatted(getState()) + "ms");
        }

        public abstract EventloopState getState();
    }

    @FlagAnnotation(Flag.HONEY_DECODE)
    class HoneyDecodeTimingsFlag extends TimingsFlag {

        @Override
        public EventloopState getState() {
            return EventloopState.HONEY_DECODE;
        }
    }

    @FlagAnnotation(Flag.SERVER_DECODE)
    class ServerDecodeTimingsFlag extends TimingsFlag {

        @Override
        public EventloopState getState() {
            return EventloopState.SERVER_DECODE;
        }
    }

    @FlagAnnotation(Flag.HONEY_HANDLE)
    class HoneyHandleTimingsFlag extends TimingsFlag {

        @Override
        public EventloopState getState() {
            return EventloopState.HONEY_HANDLE;
        }
    }

    @FlagAnnotation(Flag.SERVER_HANDLE)
    class ServerHandleTimingsFlag extends TimingsFlag {

        @Override
        public EventloopState getState() {
            return EventloopState.SERVER_HANDLE;
        }
    }

    @FlagAnnotation(Flag.NTQ_OAWO)
    class NTQOfAverageWithoutFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(TrackerUtils.getNetty().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, List<String> data) {
            data.add("ntq:o.a.w.o.:" + TrackerUtils.getNetty().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND) + "%");
        }
    }

    @FlagAnnotation(Flag.NTQ_OT)
    class NTQOfTotalFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(TrackerUtils.getNetty().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, List<String> data) {
            data.add("ntq:o.t.:" + TrackerUtils.getNetty().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND) + "%");
        }
    }

    @FlagAnnotation(Flag.STQ_OAWO)
    class STQOfAverageWithoutFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(TrackerUtils.getServer().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, List<String> data) {
            data.add("stq:o.a.w.o.:" + TrackerUtils.getServer().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND) + "%");
        }
    }

    @FlagAnnotation(Flag.STQ_OT)
    class STQOfTotalFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(TrackerUtils.getServer().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, List<String> data) {
            data.add("stq:o.t.:" + TrackerUtils.getServer().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND) + "%");
        }
    }

    @FlagAnnotation(Flag.STQ_OHST)
    class STQOfHalfServerThreadFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(TrackerUtils.getServer().getOfPacketTask(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, List<String> data) {
            data.add("o.h.s.t.:" + TrackerUtils.getServer().getOfPacketTaskFormatted(input.getPlayer(), Timespan.TEN_SECOND));
        }
    }

    @FlagAnnotation(Flag.PERFORMANCE)
    class PerformanceFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public int check(String[] args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(input.getEventloopStateTimings().getPerformance());
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, List<String> data) {
            data.add("perf:" + input.getEventloopStateTimings().formatPerformance());
        }
    }
}
