package me.alek.honey.monitor;

import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FlagRunner {

    public static class Env<WP extends WrappedPacket<WP>> implements Chipstate<PacketContainer<WP>> {

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
            return flags;
        }

        @Override
        public boolean test(PacketContainer<WP> input) {
            return this.conditions.stream().allMatch(c -> c.test(input));
        }

        @Override
        public void append(PacketContainer<WP> input, List<String> data) {
            this.conditions.forEach(c -> c.append(input, data));
        }

        @Override
        public String toString() {
            return this.conditions.toString();
        }
    }

    public static class Condition<WP extends WrappedPacket<WP>> implements Chipstate<PacketContainer<WP>> {

        public static final Predicate<Stream<Boolean>> NOT = o -> o.anyMatch(b -> !b);
        public static final Predicate<Stream<Boolean>> AND = o -> o.allMatch(b -> b);
        public static final Predicate<Stream<Boolean>> OR = o -> o.anyMatch(b -> b);

        private final Array<Chipstate<PacketContainer<WP>>> flags;
        private final Predicate<Stream<Boolean>> tester;

        public Condition(Array<Chipstate<PacketContainer<WP>>> flags, Predicate<Stream<Boolean>> tester) {
            this.flags = flags;
            this.tester = tester;
        }

        @Override
        public boolean test(PacketContainer<WP> input) {
            return this.tester.test(this.flags.stream().map(c -> c.test(input)));
        }

        @Override
        public void append(PacketContainer<WP> input, List<String> data) {
            this.flags.forEach(f -> f.append(input, data));
        }

        @Override
        public String toString() {
            return this.flags.toString();
        }
    }

    public interface Chipstate<T> extends Predicate<T> {

        void append(T input, List<String> data);

        interface Flag<T> extends Chipstate<T> {

            MonitorFlag<T> flag();
        }
    }
}
