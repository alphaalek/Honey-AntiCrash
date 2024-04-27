package me.alek.honey.monitor;

import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.IDRegistry;
import org.bukkit.Bukkit;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MonitorFlagParser {

    public interface Parsed {

        String type();

        String raw() throws WrongMonitorFlagSyntaxException;

        Object get();

        default boolean test(Array<Parsed> parsed) {
            return true;
        }

        default WrongMonitorFlagSyntaxException getWrongSyntaxException() {
            return null;
        }

        static Parsed fromOperator(LogicalOperator operator, int index) {
            return new Parsed() {

                private final AtomicReference<String> wrongSyntaxMessage = new AtomicReference<>();

                @Override
                public Object get() {
                    return operator;
                }

                @Override
                public String raw() {
                    return operator.getSymbol();
                }

                @Override
                public String type() {
                    return operator.name();
                }

                @Override
                public boolean test(Array<Parsed> parsed) {
                    return testOperator(parsed, index, operator, wrongSyntaxMessage);
                }

                @Override
                public WrongMonitorFlagSyntaxException getWrongSyntaxException() {
                    return WrongMonitorFlagSyntaxException.wrongOperatorUsage(operator, wrongSyntaxMessage.get());
                }
            };
        }

        static Parsed fromFlag(MonitorFlag<?> flag) {
            return new Parsed() {
                @Override
                public String type() {
                    return Optional.ofNullable(
                                    MonitorFlagParser.getFlag(flag)).map(MonitorFlag.Flag::getName)
                            .orElse("Unknown");
                }

                @Override
                public String raw() throws WrongMonitorFlagSyntaxException {
                    return "--" + Optional.ofNullable(getFlag(flag))
                            .map(MonitorFlag.Flag::getName)
                            .orElseThrow(() -> WrongMonitorFlagSyntaxException.wrongFlagUsage(flag));
                }

                @Override
                public Object get() {
                    return flag;
                }
            };
        }
    }

    private static boolean isParenthesesStart(Object parsed) {
        return parsed.equals(LogicalOperator.PARANTHESIS_START);
    }

    private static boolean isParanthesesEnd(Object parsed) {
        return parsed.equals(LogicalOperator.PARANTHESIS_END);
    }

    public static boolean testOperator(Array<Parsed> parsed, int index, LogicalOperator operator, AtomicReference<String> wrongSyntaxMessage) {
        if (index == parsed.size() - 1 && operator != LogicalOperator.PARANTHESIS_END && operator != LogicalOperator.NOT) {
            wrongSyntaxMessage.set("Using operator without flag.");
            return false;
        }
        if (operator != LogicalOperator.PARANTHESIS_END && operator != LogicalOperator.PARANTHESIS_START) {
            Object next = parsed.get(index + 1).get();
            if (!(next instanceof MonitorFlag || next.equals(LogicalOperator.PARANTHESIS_START) || next.equals(LogicalOperator.NOT))) {
                wrongSyntaxMessage.set("Using operator without flag.");
                return false;
            }
        }

        int i = 0;
        Array<Integer> start = new Array<>();
        Array<Integer> end = new Array<>();
        for (Parsed elem : parsed) {
            if (isParanthesesEnd(elem.get())) {
                end.add(i);
            } else if (isParenthesesStart(elem.get())) {
                start.add(i);
            }
            i++;
        }

        Bukkit.broadcastMessage(parsed.as(Parsed::get) + " " + operator + " " + start + " " + end + " " + i + " " + index);

        try {
            int unexpectedIndex = -1;
            if (start.size() > end.size()) {
                unexpectedIndex = start.get(0);
                wrongSyntaxMessage.set("Unexpected paranthesis at index " + unexpectedIndex + ", at \"" + getContextString(parsed, start.get(0)) + "\".");
            }
            if (start.size() < end.size()) {
                unexpectedIndex = end.getLast();
                wrongSyntaxMessage.set("Unexpected paranthesis at index " + unexpectedIndex + ", at \"" + getContextString(parsed, end.getLast()) + "\".");
            }
        } catch (WrongMonitorFlagSyntaxException ex) {
            wrongSyntaxMessage.set(ex.getMessage());
        }

        return start.size() == end.size();
    }

    private static String getContextString(Array<Parsed> parsed, int index) throws WrongMonitorFlagSyntaxException {
        StringBuilder builder = new StringBuilder();
        int i = index;
        while (builder.length() < 7 && i < parsed.size()) {
            builder.append(parsed.get(i++).raw());
        }
        return builder.length() > 7 ? builder.substring(0, 7) + "..." : builder.toString();
    }

    private static Array<Parsed> getMonitorFlagsAndOperators(String[] args) throws WrongMonitorFlagSyntaxException {
        if (args.length == 0) {
            return Array.empty();
        }

        Array<String> parsing = Array.from(args);
        Array<Parsed> parsed = new Array<>();

        int readIndex = 0;
        while (readIndex < parsing.size()) {
            String argument = parsing.get(readIndex++).trim();

            LogicalOperator operator = LogicalOperator.getBySymbol(argument.substring(0, 1));
            if (operator != null) {
                parsed.add(Parsed.fromOperator(operator, parsed.size()));

                if (argument.length() > 1) {
                    parsing.move(readIndex - 1, 1);
                    parsing.set(readIndex, argument.substring(1));
                }
                continue;
            }

            if (argument.startsWith("--")) {
                String flagString = argument.toLowerCase().substring(2).replaceAll("\\)", "");
                MonitorFlag.Flag flagType = MonitorFlag.Flag.getFlagByName(flagString);
                if (flagType == null) {
                    throw WrongMonitorFlagSyntaxException.wrongFlag(flagString);
                }

                @SuppressWarnings("unchecked")
                MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag =
                        (MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>) flagType.createNew();
                readIndex += flag.check(parsing, readIndex);
                parsed.add(Parsed.fromFlag(flag));

                if (argument.endsWith(")")) {
                    parsing.insert(readIndex, ")");
                }
            }
        }

        ensureCorrectParanthesesPostParsing(parsed);

        return parsed;
    }

    public static Flags.RuntimeFlags getMonitorFlags(String[] args) throws WrongMonitorFlagSyntaxException {
        Array<Object> elements = getMonitorFlagsAndOperators(args).as(Parsed::get);


        Flags.Conditions conditions = new Flags.Conditions();
        return new Flags.RuntimeFlags(Array.from(new Flags.Env(getFlagsWithin(elements, conditions), conditions)));
    }

    private static Array<Flags.Chipstate> getFlagsWithin(Array<Object> elements, Flags.Conditions conditions) {
        Array<Flags.Chipstate> flags = new Array<>();
        wrapLogicalANDs(elements);
        int chipstateIndex = 0;

        for (int i = 0; i < elements.size(); i++) {
            Object elem = elements.get(i);
            if (elem instanceof MonitorFlag) {
                chipstateIndex++;
                Flags.Runtime runtime = new Flags.Runtime((MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>) elem);
                flags.add(runtime);
            }
            if (elem.equals(LogicalOperator.NOT)) {
                conditions.addCondition(new Flags.Condition(IDRegistry.from(chipstateIndex), flags, LogicalOperator.NOT));
            }
            else if (elem.equals(LogicalOperator.AND)) {
                conditions.addCondition(new Flags.Condition(IDRegistry.from(chipstateIndex - 1, chipstateIndex), flags, LogicalOperator.AND));
            }
            else if (elem.equals(LogicalOperator.OR)) {
                conditions.addCondition(new Flags.Condition(IDRegistry.from(chipstateIndex - 1, chipstateIndex), flags, LogicalOperator.OR));
            }
            else if (elem.equals(LogicalOperator.PARANTHESIS_START)) {
                Array<Object> in = getElementsInParanthesis(elements.from(i));
                i += in.size();

                chipstateIndex++;
                Flags.Conditions newChipstateConditions = new Flags.Conditions();
                flags.add(new Flags.Env(getFlagsWithin(in, newChipstateConditions), newChipstateConditions));
            }
            else if (elem.equals(LogicalOperator.PARANTHESIS_END)) {
                return flags;
            }
        }

        return flags;
    }

    private static void wrapLogicalANDs(Array<Object> elements) {
        Array<Object> prev = elements.copy();
        boolean hasOr = false;
        for (int i = 0; i < elements.size(); i++) {
            Object element = elements.get(i);
            if (element.equals(LogicalOperator.AND)) {
                elements.insert(i++ - 1, LogicalOperator.PARANTHESIS_START);
                Array<Object> nextChipstate = getElementsInNextChipstate(elements, i);
                elements.insert(i += (nextChipstate.size() + 1), LogicalOperator.PARANTHESIS_END);
            }

            if (element.equals(LogicalOperator.OR)) {
                hasOr = true;
            }
        }

        if (!hasOr) {
            elements.clear();
            elements.replace(0, prev);
        }
    }

    private static Array<Object> getElementsInNextChipstate(Array<Object> elements, int from) {
        Array<Object> remaining = elements.from(from + 1);
        Object next = remaining.get(0);

        Array<Object> array = Array.from(next);
        if (next.equals(LogicalOperator.NOT)) {
            array.add(getElementsInNextChipstate(elements, 1));
        }
        else if (next.equals(LogicalOperator.PARANTHESIS_START)) {
            for (int i = 1; i < remaining.size(); i++) {
                Object element = remaining.get(i);
                array.add(element);
                if (element.equals(LogicalOperator.PARANTHESIS_START)) {
                    Array<Object> inner = getElementsInNextChipstate(remaining, i);
                    i += inner.size();
                    array.addAll(inner);
                }
                if (element.equals(LogicalOperator.PARANTHESIS_END)) {
                    break;
                }
            }
        }

        return array;
    }

    private static Array<Object> getElementsInParanthesis(Array<Object> elements) {
        Array<Object> within = new Array<>();
        for (int i = 1; i < elements.size(); i++) {
            Object elem = elements.get(i);
            within.add(elem);

            if (elem.equals(LogicalOperator.PARANTHESIS_END)) {
                return within;
            }
        }

        return within;
    }

    public static String getFromArgs(Array<String> args, int index) {
        String arg = args.get(index);
        if (arg.endsWith(")")) {
            args.insert(index + 1, ")");
            args.set(index, arg.substring(0, arg.length() - 1));
            return getFromArgs(args, index);
        }
        return arg;
    }

    private static void ensureCorrectParanthesesPostParsing(Array<Parsed> parsed) throws WrongMonitorFlagSyntaxException {
        for (Parsed elem : parsed) {
            if (!elem.test(parsed)) {
                throw elem.getWrongSyntaxException();
            }
        }
    }

    public static MonitorFlag.Flag getFlag(MonitorFlag<?> flag) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends MonitorFlag<?>> clazz = (Class<? extends MonitorFlag<?>>) flag.getClass();

            return clazz.getAnnotation(MonitorFlag.FlagAnnotation.class).value();
        } catch (Exception ex) {
            return null;
        }
    }
}
