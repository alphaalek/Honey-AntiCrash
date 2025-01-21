package me.alek.honey.monitor;

import me.alek.honey.lang.Lang;
import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
                    return String.valueOf(operator.getSymbol());
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
                            .orElse(Lang.get("unknown"));
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
        if (index == parsed.size() - 1
                && !operator.equals(LogicalOperator.PARANTHESIS_END)
                && !operator.equals(LogicalOperator.NOT)) {
            wrongSyntaxMessage.set(Lang.get("packetmonitor.operator.no.flag"));
            return false;
        } else if (!operator.equals(LogicalOperator.PARANTHESIS_END) && !operator.equals(LogicalOperator.PARANTHESIS_START)) {
            Object next = parsed.get(index + 1).get();
            if (!(next instanceof MonitorFlag
                    || next.equals(LogicalOperator.PARANTHESIS_START)
                    || (next.equals(LogicalOperator.NOT) && !operator.equals(LogicalOperator.NOT)))) {
                wrongSyntaxMessage.set(Lang.get("packetmonitor.operator.no.flag"));
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

        try {
            if (start.size() > end.size()) {
                wrongSyntaxMessage.set(
                        Lang.getFormatted("packetmonitor.operator.unexpected.paranthesis", start.get(0), getContextString(parsed, start.get(0))));
            }
            if (start.size() < end.size()) {
                wrongSyntaxMessage.set(
                        Lang.getFormatted("packetmonitor.operator.unexpected.paranthesis", end.getLast(), getContextString(parsed, end.getLast())));
            }
        } catch (WrongMonitorFlagSyntaxException ex) {
            wrongSyntaxMessage.set(ex.getMessage());
        }

        return start.size() == end.size();
    }

    private static String getContextString(Array<Parsed> parsed, int index) throws WrongMonitorFlagSyntaxException {
        StringBuilder builder = new StringBuilder();
        int i = index;
        while (builder.length() < 10 && i < parsed.size()) {
            builder.append(parsed.get(i++).raw());
        }
        return builder.length() > 10 ? builder.substring(0, 10) + "..." : builder.toString();
    }

    private static Array<Parsed> getMonitorFlagsAndOperators(String[] args) throws WrongMonitorFlagSyntaxException {
        if (args.length == 0) {
            return Array.empty();
        }

        Array<String> parsing = Array.from(args);
        Array<Parsed> parsed = new Array<>();

        while (!parsing.isEmpty()) {
            String argument = parsing.get(0).trim();

            // check if operator
            LogicalOperator operator = LogicalOperator.getBySymbol(argument.charAt(0));
            if (operator != null) {
                if (argument.length() > 1) {
                    parsing.set(0, String.valueOf(operator.getSymbol()));
                    parsing.insert(1, argument.substring(1));
                    // try again
                    continue;
                } else {
                    parsed.add(MonitorFlagParser.Parsed.fromOperator(operator, parsed.size()));
                }
            }
            // keep paranthesis and flags seperated
            else if (argument.endsWith(")")) {
                parsing.set(0, argument.substring(0, argument.length() - 1));
                parsing.insert(1, ")");
                // try again
                continue;
            }
            // check if flag
            else if (argument.startsWith("--")) {
                String flagString = argument.toLowerCase().substring(2).replaceAll("\\)", "");
                MonitorFlag.Flag flagType = MonitorFlag.Flag.getFlagByName(flagString);
                if (flagType == null) {
                    throw WrongMonitorFlagSyntaxException.wrongFlag(flagString);
                }

                // parse flag
                @SuppressWarnings("unchecked")
                MonitorFlag<PacketContainer<?>> flag = (MonitorFlag<PacketContainer<?>>) flagType.createNew();
                parsing = parsing.from(flag.parse(parsing.from(1)));
                parsed.add(MonitorFlagParser.Parsed.fromFlag(flag));
            }

            // go to next index
            parsing = parsing.from(1);
        }

        ensureCorrectParanthesesPostParsing(parsed);

        return parsed;
    }

    public static <WP extends WrappedPacket<WP>> FlagRunner.Env<WP> getMonitorFlags(String[] args) throws WrongMonitorFlagSyntaxException {
        Array<Object> elements = getMonitorFlagsAndOperators(args).as(MonitorFlagParser.Parsed::get);

        Array<FlagRunner.Condition<WP>> conditions = new Array<>();
        FlagRunner.Env<WP> env = new FlagRunner.Env<>(conditions);

        addMissingAnds(elements);
        applyFlagsWithin(elements, conditions);

        Bukkit.broadcastMessage("Final " + conditions);

        return env;
    }

    @SuppressWarnings("unchecked")
    private static <WP extends WrappedPacket<WP>> void applyFlagsWithin(Array<Object> elements, Array<FlagRunner.Condition<WP>> conditions) {
        Array<FlagRunner.Chipstate<PacketContainer<WP>>> flags = new Array<>();
        Bukkit.broadcastMessage("Elements " + elements);
        for (int i = 0; i < elements.size(); i++) {
            Object elem = elements.get(i);
            Bukkit.broadcastMessage(elem + "");
            if (elem instanceof MonitorFlag) {
                flags.add((MonitorFlag<PacketContainer<WP>>) elem);
            } else if (elem.equals(LogicalOperator.NOT) || elem.equals(LogicalOperator.PARANTHESIS_START)) {
                Array<Object> nextChipstateElements = getElementsInNextChipstate(elements, i + 1);
                i += nextChipstateElements.size(); // skip elements in next chipstate for parent parser

                Array<FlagRunner.Condition<WP>> newConditions = new Array<>();
                applyFlagsWithin(nextChipstateElements, newConditions);

                // add child environment to parent flags
                FlagRunner.Env<WP> env = new FlagRunner.Env<>(newConditions);
                flags.add(env);

                if (elem.equals(LogicalOperator.NOT)) {
                    Bukkit.broadcastMessage("Not " + env + " " + newConditions + " " +  nextChipstateElements );
                    conditions.add(new FlagRunner.Condition<>(Array.just(env), FlagRunner.Condition.NOT));
                }
            } else if (elem instanceof Array) {
                // result of parsing child chipstate
                Array<FlagRunner.Condition<WP>> newConditions = new Array<>();
                applyFlagsWithin((Array<Object>) elem, newConditions);

                flags.add(new FlagRunner.Env<>(newConditions));
            } else if (elem.equals(LogicalOperator.PARANTHESIS_END)) {
                break;
            }
        }

        LinkedList<FlagRunner.Chipstate<PacketContainer<WP>>> flagsQueue = Arrays.stream(flags.getHandle())
                .map(o -> (FlagRunner.Chipstate<PacketContainer<WP>>) o)
                .collect(Collectors.toCollection(LinkedList::new));
        // add logical operators to chipstates
        for (int i = 0; i < elements.size(); i++) {
            Object elem = elements.get(i);
            if (elem.equals(LogicalOperator.AND) || elem.equals(LogicalOperator.OR)) {
                FlagRunner.Chipstate<PacketContainer<WP>> flag1 = flagsQueue.pollFirst();
                FlagRunner.Chipstate<PacketContainer<WP>> flag2 = flagsQueue.pollFirst();

                FlagRunner.Condition<WP> condition = new FlagRunner.Condition<>(Array.fromElements(flag1, flag2),
                        elem.equals(LogicalOperator.AND) ? FlagRunner.Condition.AND : FlagRunner.Condition.OR);
                conditions.add(condition);

                // add new env from this condition
                FlagRunner.Env<WP> env = new FlagRunner.Env<>(Array.just(condition));
                flagsQueue.addFirst(env);
            }
        }

        // if no conditions was added, fallback to and for all flags
        if (conditions.isEmpty()) {
            conditions.add(new FlagRunner.Condition<>(flags, FlagRunner.Condition.AND));
        }
    }

    private static void addMissingAnds(Array<Object> elements) {
        for (int i = 0; i < elements.size(); i++) {
            Object elem = elements.get(i);
            if (elem.equals(LogicalOperator.PARANTHESIS_END) || elem instanceof MonitorFlag) {
                int nextIdx = i + 1;
                if (nextIdx == elements.size()) {
                    return;
                }
                if (elements.get(nextIdx).equals(LogicalOperator.NOT)) {
                    nextIdx++;
                    if (nextIdx == elements.size()) {
                        return;
                    }
                }
                if (elements.get(nextIdx).equals(LogicalOperator.PARANTHESIS_START) || elements.get(nextIdx) instanceof MonitorFlag) {
                    elements.insert(nextIdx, LogicalOperator.AND);
                }
            }
        }
    }

    private static Array<Object> getElementsInNextChipstate(Array<Object> elements, int from) {
        Array<Object> remaining = elements.from(from);
        Object next = remaining.get(0);

        Array<Object> array = Array.just(next);
        if (next.equals(LogicalOperator.PARANTHESIS_START)) {
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

    public static String getFromArgs(Array<String> args) {
        String arg = args.get(0);
        if (arg.endsWith(")")) {
            args.insert(1, ")");
            args.set(0, arg.substring(0, arg.length() - 1));
            return getFromArgs(args);
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
