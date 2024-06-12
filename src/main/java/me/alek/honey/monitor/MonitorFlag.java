package me.alek.honey.monitor;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.EventloopState;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface MonitorFlag<T> {

    enum Flag {
        PLAYER("player", "--player [player name]", false, PlayerFlag::new),
        TYPE("type", "--type [packet type]", false, TypeFlag::new),
        STATE("state", "--state [packet state]", false, StateFlag::new),
        CANCELLED("cancelled", "--cancelled", false, CancelledFlag::new),
        DETAILED("detailed", "--detailed", false, DetailedFlag::new),
        DECODE("min-decode", "--min-decode [number (as decimal) (in millis)]", false, DecodeTimingsFlag::new),
        IN_BETWEEN("min-in-between", "--min-in-between [number (as decimal) (in millis)]", false, InBetweenFlag::new),
        HONEY_HANDLE("min-honey-handle", "--min-honey-handle [number (as decimal) (in millis)]", true, HoneyHandleTimingsFlag::new),
        SERVER_HANDLE("min-server-handle", "--min-server-handle [number (as decimal) (in millis)]", true, ServerHandleTimingsFlag::new),
        NTQ_OAWO("min-ntq-oawo", "--min-ntq-oawo [number (as percentage)]", true, NTQOfAverageWithoutFlag::new),
        NTQ_OT("min-ntq-ot", "--min-ntq-ot [number (as percentage)]", true, NTQOfTotalFlag::new),
        STQ_OAWO("min-stq-oawo", "--min-stq-oawo [number (as percentage)]", true, STQOfAverageWithoutFlag::new),
        STQ_OT("min-stq-ot", "--min-stq-ot [number (as percentage)]", true, STQOfTotalFlag::new),
        STQ_OHST("min-ohst", "--min-ohst [number (as percentage)]", true, STQOfHalfServerThreadFlag::new),
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

        public static void checkSyntax(Array<String> args, int readIndex, MonitorFlag<?> flag, int takes) throws WrongMonitorFlagSyntaxException {
            if (readIndex - 1 + takes >= args.size()) {
                throw WrongMonitorFlagSyntaxException.wrongFlagUsage(flag);
            }
        }
    }

    interface NoArgumentMonitorFlag<F> extends MonitorFlag<F> {

        @Override
        default int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return 0;
        }
    }

    interface NoAppendingMonitorFlag<F> extends MonitorFlag<F> {

        @Override
        default void append(F input, Flags.Appendable appendable, List<Flags.AppendedData> data) {

        }
    }

    int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException;

    boolean test(T input);

    void append(T input, Flags.Appendable appendable, List<Flags.AppendedData> data);

    default String getWrongSyntaxMessage() {
        return "You have to choose an argument";
    }

    default String getSpecified() {
        return "";
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
        public String getSpecified() {
            return "<double> " + value;
        }

        @Override
        public int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, readIndex, flag, 1);

            try {
                this.value = Double.parseDouble(MonitorFlagParser.getFromArgs(args, readIndex));
            } catch (NumberFormatException ex) {
                throw WrongMonitorFlagSyntaxException.wrongFlagUsage(this);
            }
            return 1;
        }

        @Override
        public String getWrongSyntaxMessage() {
            return "You have to type a valid number";
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
        public String getSpecified() {
            return "<player name> " + playerName;
        }

        @Override
        public int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, readIndex, this, 1);

            this.playerName = MonitorFlagParser.getFromArgs(args, readIndex);
            return 1;
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return input.getPlayerName().equalsIgnoreCase(playerName);
        }
    }

    @FlagAnnotation(Flag.TYPE)
    class TypeFlag implements NoAppendingMonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private PacketType type;
        private String specified;

        @Override
        public String getSpecified() {
            return "<packet type> " + type;
        }

        @Override
        public int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, readIndex, this, 1);

            this.specified = MonitorFlagParser.getFromArgs(args, readIndex);
            Optional<PacketType> type = PacketTypes.getPacketType(specified, PacketTypes::isServerbound, PacketTypes.NameGetStrategy.EITHER);
            if (type.isPresent()) {
                this.type = type.get();
                return 1;
            }

            throw WrongMonitorFlagSyntaxException.wrongFlagUsage(this);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return input.getType().equals(this.type);
        }

        @Override
        public String getWrongSyntaxMessage() {
            return "Couldn't find any packet type matching \"" + Optional.ofNullable(specified).orElse("") + "\"";
        }
    }

    @FlagAnnotation(Flag.STATE)
    class StateFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private PacketState state;

        @Override
        public String getSpecified() {
            return "<packet state> " + state;
        }

        @Override
        public int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, readIndex, this, 1);

            String specified = MonitorFlagParser.getFromArgs(args, readIndex);
            for (PacketState state : PacketState.values()) {
                if (state != PacketState.UNKNOWN && state.name().equalsIgnoreCase(specified)) {
                    this.state = state;
                    return 1;
                }
            }

            throw WrongMonitorFlagSyntaxException.wrongFlagUsage(this);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return state == PacketState.ANY || (input.getState().equals(state));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, Flags.Appendable appendable, List<Flags.AppendedData> data) {
            data.add(Flags.AppendedData.from(input.getProtocolState().name(), appendable));
        }
    }

    @FlagAnnotation(Flag.CANCELLED)
    class CancelledFlag implements NoAppendingMonitorFlag<PacketContainer<? extends WrappedPacket<?>>>, NoArgumentMonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return input.isCancelled();
        }
    }

    @FlagAnnotation(Flag.DETAILED)
    class DetailedFlag implements NoArgumentMonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return true;
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, Flags.Appendable appendable, List<Flags.AppendedData> data) {
            data.add(Flags.AppendedData.from(HoneyPlugin.get().getLoggerService().formatPacketEntries(input), appendable));
        }
    }

    abstract class TimingsFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(input.getEventloopStateTimings().getTimeForMillis(getState()));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, Flags.Appendable appendable, List<Flags.AppendedData> data) {
            data.add(Flags.AppendedData.from(format(getState()) + "=" + input.getEventloopStateTimings().getTimeForMillisFormatted(getState()) + "ms", appendable));
        }

        public String format(EventloopState state) {
            return state.toString().toLowerCase().replaceAll("_", "-");
        }

        public abstract EventloopState getState();
    }

    @FlagAnnotation(Flag.DECODE)
    class DecodeTimingsFlag extends TimingsFlag {

        @Override
        public EventloopState getState() {
            return EventloopState.DECODE;
        }
    }

    @FlagAnnotation(Flag.IN_BETWEEN)
    class InBetweenFlag extends TimingsFlag {

        @Override
        public EventloopState getState() {
            return EventloopState.IN_BETWEEN;
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
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(TrackerUtils.getNetty().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, Flags.Appendable appendable, List<Flags.AppendedData> data) {
            data.add(Flags.AppendedData.from("ntq-oawo=" + TrackerUtils.getNetty().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND) + "%", appendable));
        }
    }

    @FlagAnnotation(Flag.NTQ_OT)
    class NTQOfTotalFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(TrackerUtils.getNetty().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, Flags.Appendable appendable, List<Flags.AppendedData> data) {
            data.add(Flags.AppendedData.from("ntq-ot=" + TrackerUtils.getNetty().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND) + "%", appendable));
        }
    }

    @FlagAnnotation(Flag.STQ_OAWO)
    class STQOfAverageWithoutFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(TrackerUtils.getServer().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, Flags.Appendable appendable, List<Flags.AppendedData> data) {
            data.add(Flags.AppendedData.from("stq-oawo=" + TrackerUtils.getServer().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND) + "%", appendable));
        }
    }

    @FlagAnnotation(Flag.STQ_OT)
    class STQOfTotalFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(TrackerUtils.getServer().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, Flags.Appendable appendable, List<Flags.AppendedData> data) {
            data.add(Flags.AppendedData.from("stq-ot=" + TrackerUtils.getServer().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND) + "%", appendable));
        }
    }

    @FlagAnnotation(Flag.STQ_OHST)
    class STQOfHalfServerThreadFlag implements MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int check(Array<String> args, int readIndex) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.check(args, readIndex);
        }

        @Override
        public boolean test(PacketContainer<? extends WrappedPacket<?>> input) {
            return numberArgumentFlag.test(TrackerUtils.getServer().getOfPacketTask(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<? extends WrappedPacket<?>> input, Flags.Appendable appendable, List<Flags.AppendedData> data) {
            data.add(Flags.AppendedData.from("stq-ohst=" + TrackerUtils.getServer().getOfPacketTaskFormatted(input.getPlayer(), Timespan.TEN_SECOND), appendable));
        }
    }
}
