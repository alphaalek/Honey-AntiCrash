package me.alek.honey.monitor;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.util.Array;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlagRunner {

    public static class Env<WP extends SourcedWrappedPacket<WP>> implements Chipstate<PacketContainer<WP>> {

        private final Array<Condition<WP>> conditions;

        public Env(Array<Condition<WP>> conditions) {
            this.conditions = conditions;
        }

        public List<MonitorFlag<PacketContainer<WP>>> getAllFlags() {
            List<MonitorFlag<PacketContainer<WP>>> flags = new ArrayList<>();
            for (Condition<WP> condition : this.conditions) {
                for (Chipstate<PacketContainer<WP>> chipstate : condition.flags) {
                    if (chipstate instanceof Env) {
                        flags.addAll(((Env<WP>)chipstate).getAllFlags());
                    } else if (chipstate instanceof MonitorFlag) {
                        flags.add((MonitorFlag<PacketContainer<WP>>) chipstate);
                    }
                }
            }
            return flags.stream().distinct().collect(Collectors.toList());
        }

        public void append(PacketContainer<WP> input, List<String> data) {
            getAllFlags().forEach(f -> f.append(input, data));
        }

        @Override
        public boolean test(PacketContainer<WP> input) {
            return this.conditions.stream().allMatch(c -> c.test(input));
        }

        @Override
        public String toString() {
            return "Env{" + this.conditions.toString()
                    .replaceAll("\\[", "")
                    .replaceAll("]", "") + "}";
        }
    }

    public static class Condition<WP extends SourcedWrappedPacket<WP>> implements Chipstate<PacketContainer<WP>> {

        public enum Match {
            NOT(o -> o.noneMatch(b -> b)),
            AND(o -> o.allMatch(b -> b)),
            OR(o -> o.anyMatch(b -> b)),
            ;
            private final Predicate<Stream<Boolean>> matcher;

            Match(Predicate<Stream<Boolean>> matcher) {
                this.matcher = matcher;
            }
        }

        private final Array<Chipstate<PacketContainer<WP>>> flags;
        private final Match match;

        public Condition(Array<Chipstate<PacketContainer<WP>>> flags, Match match) {
            this.flags = flags;
            this.match = match;
        }

        @Override
        public boolean test(PacketContainer<WP> input) {
            return this.match.matcher.test(this.flags.stream().map(c -> c.test(input)));
        }

        @Override
        public String toString() {
            return "Condition{" + this.match + ", " + this.flags.toString()
                    .replaceAll("\\[", "")
                    .replaceAll("]", "") + "}";
        }
    }

    public interface Chipstate<T> extends Predicate<T> {

        interface Flag<T> extends Chipstate<T> {

            MonitorFlag<T> flag();
        }
    }
}
