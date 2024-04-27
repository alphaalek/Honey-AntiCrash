package me.alek.honey.monitor;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.CallbackAction;
import me.alek.honey.util.IDRegistry;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Flags {

    public static class RuntimeFlags implements Iterable<Chipstate> {

        private int processId = 0;
        private final Array<Chipstate> flags;

        public RuntimeFlags(Array<Chipstate> flags) {
            this.flags = flags;

            IDRegistry registry = new IDRegistry();
            for (Chipstate flag : flags) {
                flag.assignId(registry.acquireID());
            }
        }

        public Array<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> getFlags() {
            return Array.from(flags, Chipstate::getFlags);
        }

        public boolean isEmpty() {
            return flags.isEmpty();
        }

        @Override
        public Iterator<Chipstate> iterator() {
            return flags.iterator();
        }

        public synchronized boolean process(PacketContainer<? extends WrappedPacket<?>> input, List<String> data) {
            Array<Processable> triggered = new Array<>();
            processId++;

            Bukkit.broadcastMessage("Processing flags");

            for (Chipstate flag : flags) {

                if (!flag.isProcessed(processId)) {
                    flag.process(processId, input, triggered, data);
                }

                try {
                    if (!flag.getOutput(processId)) {
                        return false;
                    }
                } catch (Processable.GetOutputWithoutProcessException ex) {
                    return false;
                }
            }
            return true;
        }
    }

    public interface Processable {

        class GetOutputWithoutProcessException extends Exception {

        }

        boolean isProcessed(int id);

        boolean getOutput(int id) throws GetOutputWithoutProcessException;

        void process(int id, PacketContainer<? extends WrappedPacket<?>> input, Array<Processable> triggered, List<String> data);
    }

    static class Own implements Chipstate {

        private final Conditions conditions;
        private final Array<Chipstate> flags;

        private int lastProcessedID;
        private boolean output;

        public Own(Array<Chipstate> flags, Conditions conditions) {
            this.flags = flags;
            this.conditions = conditions;
        }

        public Own(Array<Chipstate> flags) {
            this(flags, new Conditions());
        }

        @Override
        public void assignId(int id) {
        }

        @Override
        public Array<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> getFlags() {
            return Array.from(flags, Chipstate::getFlags);
        }

        @Override
        public Array<Chipstate> getChipstates() {
            return flags;
        }

        @Override
        public boolean isProcessed(int id) {
            return lastProcessedID == id;
        }

        @Override
        public boolean getOutput(int id) {
            return output;
        }

        @Override
        public void process(int id, PacketContainer<? extends WrappedPacket<?>> input, Array<Processable> triggered, List<String> data) {
            lastProcessedID = id;
            for (Chipstate flag : flags) {

                Bukkit.broadcastMessage("Processing chipstate from own " + flag);

                flag.process(id, input, triggered, data);

                try {
                    this.output = flag.getOutput(id);
                    Bukkit.broadcastMessage("Processed from own " + output + " " + flag);
                } catch (GetOutputWithoutProcessException ex) {
                    return;
                }
            }

            conditions.test(id, input);
        }
    }

    public interface Chipstate extends Processable {

        void assignId(int id);

        Array<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> getFlags();

        Array<Chipstate> getChipstates();
    }

    public static class Condition {

        private final IDRegistry triggerPositions;
        private final LogicalOperator delegatedOperator;
        private final Array<Chipstate> flags;

        public Condition(IDRegistry triggerPositions, Array<Chipstate> flags, LogicalOperator delegatedOperator) {
            this.triggerPositions = triggerPositions;
            this.flags = flags;
            this.delegatedOperator = delegatedOperator;
        }

        public boolean test(int id, PacketContainer<? extends WrappedPacket<?>> input) {
            Bukkit.broadcastMessage("Checking condition " + input);

            Array<Boolean> triggers = new Array<>();
            for (int i = 0; i < triggerPositions.size(); i++) {
                if (triggerPositions.hasID(i)) {
                    Processable processable = flags.get(i);

                    try {
                        triggers.add(processable.getOutput(id));
                    } catch (Processable.GetOutputWithoutProcessException ex) {
                        return false;
                    }
                }
            }

            Bukkit.broadcastMessage(triggers + " " + delegatedOperator + " " + Arrays.toString(triggerPositions.getIDs()));

            if (triggers.isEmpty()) {
                return true;
            }

            return checkOperator(triggers);
        }

        private boolean checkOperator(Array<Boolean> triggers) {
            switch (delegatedOperator) {
                case OR:
                    for (boolean trigger : triggers) {
                        if (trigger) return true;
                    }
                    return false;
                case AND:
                    for (boolean trigger : triggers) {
                        if (!trigger) return false;
                    }
                    return true;
                case NOT:
                    return !triggers.get(0);
                default:
                    return true;
            }
        }
    }

    public static class Conditions {

        private final List<Condition> triggers = new ArrayList<>();

        public void addCondition(Condition condition) {
            triggers.add(condition);
        }

        public boolean test(int id, PacketContainer<? extends WrappedPacket<?>> input) {
            Bukkit.broadcastMessage("Checking conditions " + triggers);

            for (Condition condition : triggers) {
                if (!condition.test(id, input)) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class Runtime implements Chipstate {

        private final MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag;
        private int flagId;

        private boolean output;
        private int lastProcessId;

        public Runtime(MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag) {
            this.flag = flag;
        }

        public void assignId(int id) {
            this.flagId = id;
        }

        public MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> getFlag() {
            return flag;
        }

        public void process(int id, PacketContainer<? extends WrappedPacket<?>> input, Array<Processable> triggered, List<String> data) {
            if (isProcessed(id)) {
                return;
            }

            triggered.set(flagId, this);
            lastProcessId = id;

            this.output = flag.test(input);
        }

        @Override
        public boolean isProcessed(int id) {
            return lastProcessId == id;
        }

        @Override
        public boolean getOutput(int id) throws Processable.GetOutputWithoutProcessException {
            if (!isProcessed(id)) {
                throw new Processable.GetOutputWithoutProcessException();
            }
            return output;
        }

        @Override
        public Array<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> getFlags() {
            return Array.from(flag);
        }

        @Override
        public Array<Chipstate> getChipstates() {
            return (Array<Chipstate>) (Array<?>) getFlags();
        }
    }
}
