package me.alek.honey.monitor;

import me.alek.honey.lang.Lang;
import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
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
        if (operator.equals(LogicalOperator.AND) || operator.equals(LogicalOperator.OR)) {
            Object prev;
            Object next;
            if (index == 0
                    || !((prev = parsed.get(index - 1).get()).equals(LogicalOperator.PARANTHESIS_END) || prev instanceof MonitorFlag)) {
                wrongSyntaxMessage.set(Lang.get("packetmonitor.operator.no.flag"));
                return false;
            } else if (index == parsed.size() - 1
                    || (next = parsed.get(index + 1).get()).equals(LogicalOperator.PARANTHESIS_END)
                    || next.equals(LogicalOperator.AND)
                    || next.equals(LogicalOperator.OR)) {
                wrongSyntaxMessage.set(Lang.get("packetmonitor.operator.no.flag"));
                return false;
            }
        } else if (operator.equals(LogicalOperator.NOT)) {
            Object next;
            if (index == parsed.size() - 1
                    || !(next = parsed.get(index + 1).get()).equals(LogicalOperator.PARANTHESIS_START) || next instanceof MonitorFlag) {
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
            } else if (start.size() < end.size()) {
                wrongSyntaxMessage.set(
                        Lang.getFormatted("packetmonitor.operator.unexpected.paranthesis", end.getLast(), getContextString(parsed, end.getLast())));
            } else if (index == 0 && operator.equals(LogicalOperator.PARANTHESIS_END)) {
                wrongSyntaxMessage.set(
                        Lang.getFormatted("packetmonitor.operator.unexpected.paranthesis", 0, getContextString(parsed, 0)));
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
                int use = flag.parse(parsing.from(1));
                if (use > 1) {
                    parsing = parsing.from(use - 1);
                }
                parsed.add(MonitorFlagParser.Parsed.fromFlag(flag));
            }
            // go to next index
            parsing = parsing.from(1);
        }

        ensureCorrectParanthesesPostParsing(parsed);

        return parsed;
    }

    public static <WP extends SourcedWrappedPacket<WP>> FlagRunner.Env<WP> getMonitorFlags(String[] args) throws WrongMonitorFlagSyntaxException {
        Array<Object> elements = getMonitorFlagsAndOperators(args).as(MonitorFlagParser.Parsed::get);

        Array<FlagRunner.Condition<WP>> conditions = new Array<>();
        FlagRunner.Env<WP> env = new FlagRunner.Env<>(conditions);

        addMissingAnds(elements);
        applyFlagsWithin(elements, conditions);

        return env;
    }

    @SuppressWarnings("unchecked")
    private static <WP extends SourcedWrappedPacket<WP>> void applyFlagsWithin(Array<Object> elements, Array<FlagRunner.Condition<WP>> conditions) {
        Array<FlagRunner.Chipstate<PacketContainer<WP>>> flags = new Array<>();
        Array<Object> postElements = new Array<>();

        for (int i = 0; i < elements.size(); i++) {
            Object elem = elements.get(i);
            if (elem instanceof MonitorFlag) {
                flags.add((MonitorFlag<PacketContainer<WP>>) elem);
            } else if (elem.equals(LogicalOperator.NOT) || elem.equals(LogicalOperator.PARANTHESIS_START)) {
                Array<Object> nextChipstateElements = getElementsInNextChipstate(elements, i);
                // skip elements in next chipstate for parent parser
                i += nextChipstateElements.size();
                if (nextChipstateElements.size() > 1) {
                    i++; // for paranthesis end
                }

                Array<FlagRunner.Condition<WP>> newConditions = new Array<>();
                applyFlagsWithin(nextChipstateElements, newConditions);

                FlagRunner.Env<WP> env = new FlagRunner.Env<>(newConditions);

                if (elem.equals(LogicalOperator.NOT)) {
                    Array<FlagRunner.Condition<WP>> notCondition = Array.just(new FlagRunner.Condition<>(Array.just(env), FlagRunner.Condition.Match.NOT));
                    FlagRunner.Env<WP> notEnv = new FlagRunner.Env<>(notCondition);
                    flags.add(notEnv);
                } else {
                    flags.add(env);
                }
            } else if (elem.equals(LogicalOperator.PARANTHESIS_END)) {
                break;
            } else if (elem.equals(LogicalOperator.AND) || elem.equals(LogicalOperator.OR)) {
                postElements.add(elem);
            }
        }

        // add logical operators to chipstates
        LinkedList<FlagRunner.Chipstate<PacketContainer<WP>>> flagsQueue = Arrays.stream(flags.getHandle())
                .map(o -> (FlagRunner.Chipstate<PacketContainer<WP>>) o)
                .collect(Collectors.toCollection(LinkedList::new));
        for (int i = 0; i < postElements.size(); i++) {
            Object elem = postElements.get(i);
            FlagRunner.Chipstate<PacketContainer<WP>> flag1 = flagsQueue.pollFirst();
            FlagRunner.Chipstate<PacketContainer<WP>> flag2 = flagsQueue.pollFirst();

            FlagRunner.Condition<WP> condition = new FlagRunner.Condition<>(Array.fromElements(flag1, flag2),
                    elem.equals(LogicalOperator.AND) ? FlagRunner.Condition.Match.AND : FlagRunner.Condition.Match.OR);
            conditions.add(condition);

            // add new env from this condition
            FlagRunner.Env<WP> env = new FlagRunner.Env<>(Array.just(condition));
            flagsQueue.addFirst(env);
        }

        // if no conditions was added, fallback to and for all flags
        if (conditions.isEmpty()) {
            conditions.add(new FlagRunner.Condition<>(flags, FlagRunner.Condition.Match.AND));
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

        if (next.equals(LogicalOperator.PARANTHESIS_START)) {
            Array<Object> array = new Array<>();
            for (int i = 1; i < remaining.size(); i++) {
                Object element = remaining.get(i);
                if (element.equals(LogicalOperator.PARANTHESIS_END)) {
                    break;
                }

                array.add(element);
                if (element.equals(LogicalOperator.PARANTHESIS_START)) {
                    Array<Object> inner = getElementsInNextChipstate(remaining, i);
                    i += inner.size();
                    array.addAll(inner);
                    array.add(LogicalOperator.PARANTHESIS_END);
                }
            }
            return array;
        } else if (next.equals(LogicalOperator.NOT)) {
            return getElementsInNextChipstate(remaining, 1);
        } else {
            return Array.just(next);
        }
    }

    public static String getFromArgs(Array<String> args) {
        String arg = args.get(0);
        if (arg.endsWith(")")) {
            args.set(0, arg.substring(0, arg.length() - 1));
            args.insert(1, ")");
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
