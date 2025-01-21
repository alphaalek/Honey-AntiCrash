package me.alek.honey.monitor;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.packet.LifecycleState;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.tracker.Timespan;
import me.alek.honey.util.Array;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public interface MonitorFlag<T> extends FlagRunner.Chipstate.Flag<T> {

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

        public static void checkSyntax(Array<String> args, MonitorFlag<?> flag, int takes) throws WrongMonitorFlagSyntaxException {
            if (takes > args.size()) {
                throw WrongMonitorFlagSyntaxException.wrongFlagUsage(flag);
            }
        }
    }

    interface NoArgumentMonitorFlag<F> extends MonitorFlag<F> {

        @Override
        default int parse(Array<String> args) {
            return 0;
        }
    }

    interface NoAppendingMonitorFlag<F> extends MonitorFlag<F> {

        @Override
        default void append(F input, List<String> data) {

        }
    }

    int parse(Array<String> args) throws WrongMonitorFlagSyntaxException;

    boolean test(T input);

    void append(T input, List<String> data);

    default MonitorFlag<T> flag() {
        return this;
    }

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
        public int parse(Array<String> args) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, flag, 1);

            try {
                this.value = Double.parseDouble(MonitorFlagParser.getFromArgs(args));
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
    class PlayerFlag implements NoAppendingMonitorFlag<PacketContainer<?>> {

        private String playerName;

        @Override
        public String getSpecified() {
            return "<player name> " + playerName;
        }

        @Override
        public int parse(Array<String> args) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, this, 1);

            this.playerName = MonitorFlagParser.getFromArgs(args);
            return 1;
        }

        @Override
        public boolean test(PacketContainer<?> input) {
            return input.getPlayerName().equalsIgnoreCase(playerName);
        }
    }

    @FlagAnnotation(Flag.TYPE)
    class TypeFlag implements NoAppendingMonitorFlag<PacketContainer<?>> {

        private PacketType type;
        private String specified;

        @Override
        public String getSpecified() {
            return "<packet type> " + type;
        }

        @Override
        public int parse(Array<String> args) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, this, 1);

            this.specified = MonitorFlagParser.getFromArgs(args);
            Optional<PacketType> type = PacketTypes.getPacketType(specified, PacketTypes::isServerbound, PacketTypes.NameGetStrategy.EITHER);
            if (type.isPresent()) {
                this.type = type.get();
                return 1;
            }

            throw WrongMonitorFlagSyntaxException.wrongFlagUsage(this);
        }

        @Override
        public boolean test(PacketContainer<?> input) {
            return input.getType().equals(this.type);
        }

        @Override
        public String getWrongSyntaxMessage() {
            return "Couldn't find any packet type matching \"" + Optional.ofNullable(specified).orElse("") + "\"";
        }
    }

    @FlagAnnotation(Flag.STATE)
    class StateFlag implements MonitorFlag<PacketContainer<?>> {

        private PacketState state;

        @Override
        public String getSpecified() {
            return "<packet state> " + state;
        }

        @Override
        public int parse(Array<String> args) throws WrongMonitorFlagSyntaxException {
            Flag.checkSyntax(args, this, 1);

            String specified = MonitorFlagParser.getFromArgs(args);
            for (PacketState state : PacketState.values()) {
                if (state != PacketState.UNKNOWN && state.name().equalsIgnoreCase(specified)) {
                    this.state = state;
                    return 1;
                }
            }

            throw WrongMonitorFlagSyntaxException.wrongFlagUsage(this);
        }

        @Override
        public boolean test(PacketContainer<?> input) {
            return state == PacketState.ANY || input.getState().equals(state);
        }

        @Override
        public void append(PacketContainer<?> input, List<String> data) {
            data.add(input.getState().name());
        }
    }

    @FlagAnnotation(Flag.CANCELLED)
    class CancelledFlag implements NoAppendingMonitorFlag<PacketContainer<?>>, NoArgumentMonitorFlag<PacketContainer<?>> {

        @Override
        public boolean test(PacketContainer<?> input) {
            return input.isCancelled();
        }
    }

    @FlagAnnotation(Flag.DETAILED)
    class DetailedFlag implements NoArgumentMonitorFlag<PacketContainer<?>> {

        @Override
        public boolean test(PacketContainer<?> input) {
            return true;
        }

        @Override
        public void append(PacketContainer<?> input, List<String> data) {
            data.add(HoneyPlugin.get().getLogger().formatPacketEntries(input));
        }
    }

    abstract class TimingsFlag implements MonitorFlag<PacketContainer<?>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int parse(Array<String> args) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.parse(args);
        }

        @Override
        public boolean test(PacketContainer<?> input) {
            return numberArgumentFlag.test(input.getEventloopStateTimings().getTimeForMillis(getState()));
        }

        @Override
        public void append(PacketContainer<?> input, List<String> data) {
            data.add(format(getState()) + "=" + input.getEventloopStateTimings().getTimeForMillisFormatted(getState()) + "ms");
        }

        public String format(LifecycleState state) {
            return state.toString().toLowerCase().replaceAll("_", "-");
        }

        public abstract LifecycleState getState();
    }

    @FlagAnnotation(Flag.DECODE)
    class DecodeTimingsFlag extends TimingsFlag {

        @Override
        public LifecycleState getState() {
            return LifecycleState.DECODE;
        }
    }

    @FlagAnnotation(Flag.IN_BETWEEN)
    class InBetweenFlag extends TimingsFlag {

        @Override
        public LifecycleState getState() {
            return LifecycleState.IN_BETWEEN;
        }
    }

    @FlagAnnotation(Flag.HONEY_HANDLE)
    class HoneyHandleTimingsFlag extends TimingsFlag {

        @Override
        public LifecycleState getState() {
            return LifecycleState.HONEY_HANDLE;
        }
    }

    @FlagAnnotation(Flag.SERVER_HANDLE)
    class ServerHandleTimingsFlag extends TimingsFlag {

        @Override
        public LifecycleState getState() {
            return LifecycleState.SERVER_HANDLE;
        }
    }

    @FlagAnnotation(Flag.NTQ_OAWO)
    class NTQOfAverageWithoutFlag implements MonitorFlag<PacketContainer<?>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int parse(Array<String> args) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.parse(args);
        }

        @Override
        public boolean test(PacketContainer<?> input) {
            return numberArgumentFlag.test(TrackerCollection.getNetty().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<?> input, List<String> data) {
            data.add("ntq-oawo=" + TrackerCollection.getNetty().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND) + "%");
        }
    }

    @FlagAnnotation(Flag.NTQ_OT)
    class NTQOfTotalFlag implements MonitorFlag<PacketContainer<?>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int parse(Array<String> args) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.parse(args);
        }

        @Override
        public boolean test(PacketContainer<?> input) {
            return numberArgumentFlag.test(TrackerCollection.getNetty().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<?> input, List<String> data) {
            data.add("ntq-ot=" + TrackerCollection.getNetty().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND) + "%");
        }
    }

    @FlagAnnotation(Flag.STQ_OAWO)
    class STQOfAverageWithoutFlag implements MonitorFlag<PacketContainer<?>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int parse(Array<String> args) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.parse(args);
        }

        @Override
        public boolean test(PacketContainer<?> input) {
            return numberArgumentFlag.test(TrackerCollection.getServer().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<?> input, List<String> data) {
            data.add("stq-oawo=" + TrackerCollection.getServer().getOfAverageWithout(input.getPlayer(), Timespan.TEN_SECOND) + "%");
        }
    }

    @FlagAnnotation(Flag.STQ_OT)
    class STQOfTotalFlag implements MonitorFlag<PacketContainer<?>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int parse(Array<String> args) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.parse(args);
        }

        @Override
        public boolean test(PacketContainer<?> input) {
            return numberArgumentFlag.test(TrackerCollection.getServer().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<?> input, List<String> data) {
            data.add("stq-ot=" + TrackerCollection.getServer().getOfTotal(input.getPlayer(), Timespan.TEN_SECOND) + "%");
        }
    }

    @FlagAnnotation(Flag.STQ_OHST)
    class STQOfHalfServerThreadFlag implements MonitorFlag<PacketContainer<?>> {

        private final MonitorFlag<Double> numberArgumentFlag = new NumberArgumentFlag(this);

        @Override
        public String getSpecified() {
            return numberArgumentFlag.getSpecified();
        }

        @Override
        public int parse(Array<String> args) throws WrongMonitorFlagSyntaxException {
            return numberArgumentFlag.parse(args);
        }

        @Override
        public boolean test(PacketContainer<?> input) {
            return numberArgumentFlag.test(TrackerCollection.getServer().getOfPacketTask(input.getPlayer(), Timespan.TEN_SECOND));
        }

        @Override
        public void append(PacketContainer<?> input, List<String> data) {
            data.add("stq-ohst=" + TrackerCollection.getServer().getOfPacketTaskFormatted(input.getPlayer(), Timespan.TEN_SECOND));
        }
    }
}
