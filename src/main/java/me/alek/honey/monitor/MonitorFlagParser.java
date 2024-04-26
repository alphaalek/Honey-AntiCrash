package me.alek.honey.monitor;

import me.alek.honey.monitor.exceptions.WrongMonitorFlagSyntaxException;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
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
                            .orElseThrow(() -> new WrongMonitorFlagSyntaxException(flag));
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
        if (index == parsed.size() - 1 && operator != LogicalOperator.PARANTHESIS_END) {
            wrongSyntaxMessage.set("Using operator without flag.");
            return false;
        }

        Array<Parsed> check;
        switch (operator) {
            case PARANTHESIS_START: {
                check = parsed.from(index);
                break;
            }
            case PARANTHESIS_END: {
                check = parsed.from(0, index + 1);
                break;
            }
            default: return true;
        }

        int i = 0;
        Array<Integer> start = new Array<>();
        Array<Integer> end = new Array<>();
        for (Parsed elem : check) {
            if (isParanthesesEnd(elem.get())) {
                end.add(i);
            } else if (isParenthesesStart(elem.get())) {
                start.add(i);
            }
            i++;
        }

        try {
            if (start.size() > end.size()) {
                wrongSyntaxMessage.set("Unexpected paranthesis at index " + start.get(0) + ", at \"" + getContextString(parsed, start.get(0)) + "\".");
            }
            if (start.size() < end.size()) {
                wrongSyntaxMessage.set("Unexpected paranthesis at index " + end.getLast() + ", at \"" + getContextString(parsed, end.getLast()) + "\".");
            }
        } catch (WrongMonitorFlagSyntaxException ex) {
            wrongSyntaxMessage.set(ex.getMessage());
        }

        return start.size() == end.size();
    }

    private static String getContextString(Array<Parsed> parsed, int index) throws WrongMonitorFlagSyntaxException {
        StringBuilder builder = new StringBuilder();
        int i = index;
        while (builder.length() < 5 && i < parsed.size()) {
            builder.append(parsed.get(i++).raw());
        }
        return builder.length() > 5 ? builder.substring(0, 5) + "..." : builder.toString();
    }

    public static Array<Object> getMonitorFlags(String[] args) throws WrongMonitorFlagSyntaxException {
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

        return parsed.as(Parsed::get);
    }

    public static String getFromArgs(Array<String> args, int index) {
        String arg = args.get(index);
        if (arg.endsWith(")")) {
            args.insert(index + 1, ")");
        }
        return arg.replaceAll("\\)", "");
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
