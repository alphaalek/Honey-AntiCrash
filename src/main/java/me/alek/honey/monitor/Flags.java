package me.alek.honey.monitor;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;
import me.alek.honey.util.IDRegistry;
import org.bukkit.Bukkit;

import java.util.*;

public class Flags {

    public static class RuntimeFlags implements Iterable<Chipstate> {

        private int processId = 0;
        private final Array<Chipstate> flags;

        public RuntimeFlags(Array<Chipstate> flags) {
            this.flags = flags;
        }

        public Array<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> getFlags() {
            return Array.from(flags, Chipstate::getFlags);
        }

        public boolean isEmpty() {
            return getFlags().isEmpty();
        }

        @Override
        public Iterator<Chipstate> iterator() {
            return flags.iterator();
        }

        public synchronized boolean process(PacketContainer<? extends WrappedPacket<?>> input, List<AppendedData> data) {
            processId++;
            for (Chipstate flag : flags) {
                if (!flag.isProcessed(processId)) {
                    flag.process(processId, input, data);
                }

                try {
                    if (!flag.getOutput(processId)) {
                        return false;
                    }
                } catch (Processable.GetOutputWithoutProcessException ex) {
                    ex.printStackTrace();
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

        void process(int id, PacketContainer<? extends WrappedPacket<?>> input, List<AppendedData> data);

        void toggle(int id) throws GetOutputWithoutProcessException;
    }

    static class Env implements Chipstate {

        private final Conditions conditions;
        private final Array<Chipstate> flags;

        private int lastProcessedID;
        private boolean appending;
        private boolean output;

        public Env(Array<Chipstate> flags, Conditions conditions) {
            this.flags = flags;
            this.conditions = conditions;
        }

        @Override
        public Array<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> getFlags() {
            return Array.from(flags, Chipstate::getFlags);
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
        public void process(int id, PacketContainer<? extends WrappedPacket<?>> input, List<AppendedData> data) {
            lastProcessedID = id;
            for (Chipstate flag : flags) {
                flag.process(id, input, data);
            }

            if (conditions.shouldAddANDCondition()) {
                conditions.addCondition(new Condition(IDRegistry.create(flags.size()), flags, LogicalOperator.AND));
            }

            this.output = conditions.test(id);
            if (this.output) {
                acceptAppend();
            }
        }

        @Override
        public void toggle(int id) throws GetOutputWithoutProcessException {
            if (!isProcessed(id)) {
                throw new GetOutputWithoutProcessException();
            }

            output = !output;
        }

        @Override
        public boolean shouldAppend() {
            return appending;
        }

        @Override
        public void acceptAppend() {
            appending = true;

            for (Chipstate flag : flags) {
                flag.acceptAppend();
            }
        }
    }

    public interface Chipstate extends Processable, Appendable {

        Array<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> getFlags();

    }

    public static class Condition {

        private final IDRegistry triggerPositions;
        private final LogicalOperator delegatedOperator;
        private final Array<Chipstate> flags;

        private int lastCheckedID;
        private boolean lastCheck;

        public Condition(IDRegistry triggerPositions, Array<Chipstate> flags, LogicalOperator delegatedOperator) {
            this.triggerPositions = triggerPositions;
            this.flags = flags;
            this.delegatedOperator = delegatedOperator;
        }

        public boolean test(int id) {
            if (lastCheckedID == id) {
                return lastCheck;
            }

            lastCheckedID = id;
            Array<Boolean> triggers = new Array<>();
            for (int i = 0; i < triggerPositions.size(); i++) {
                if (triggerPositions.hasID(i)) {
                    Processable processable = flags.get(i);

                    try {
                        triggers.add(processable.getOutput(id));

                        if (delegatedOperator.equals(LogicalOperator.NOT)) {
                            processable.toggle(id);
                        }

                    } catch (Processable.GetOutputWithoutProcessException ex) {
                        return lastCheck = false;
                    }
                }
            }

            if (triggers.isEmpty()) {
                return lastCheck = true;
            }

            return lastCheck = checkOperator(triggers);
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
                    for (boolean trigger : triggers) {
                        if (trigger) return false;
                    }
                    return true;
                default:
                    return true;
            }
        }

        public LogicalOperator getOperator() {
            return delegatedOperator;
        }
    }

    public static class Conditions {

        private final List<Condition> triggers = new ArrayList<>();

        public void addCondition(Condition condition) {
            triggers.add(condition);

            triggers.sort(new Comparator<Condition>() {
                @Override
                public int compare(Condition o1, Condition o2) {
                    boolean o1Not = o1.getOperator().equals(LogicalOperator.NOT);
                    boolean o2Not = o2.getOperator().equals(LogicalOperator.NOT);

                    if (o1Not && !o2Not) {
                        return -1;
                    } else if (!o1Not && o2Not) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
        }

        public boolean shouldAddANDCondition() {
            return triggers.isEmpty()
                    || triggers.size() == 1
                    && triggers.get(0).getOperator().equals(LogicalOperator.NOT);
        }

        public boolean test(int id) {
            for (Condition condition : triggers) {
                if (!condition.test(id)) {
                    return false;
                }
            }

            return true;
        }
    }

    public interface Appendable {

        boolean shouldAppend();

        void acceptAppend();

    }

    public static class AppendedData {

        static AppendedData from(String data, Appendable appendable) {
            return new AppendedData(data, appendable);
        }

        private final String data;
        private final Appendable appendable;

        public AppendedData(String data, Appendable appendable) {
            this.data = data;
            this.appendable = appendable;
        }

        public String getData() {
            return data;
        }

        public boolean shouldAppend() {
            return appendable.shouldAppend();
        }
    }

    public static class Runtime implements Chipstate {

        private final MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag;

        private boolean output;
        private boolean appending;
        private int lastProcessId;

        public Runtime(MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> flag) {
            this.flag = flag;
        }

        public MonitorFlag<PacketContainer<? extends WrappedPacket<?>>> getFlag() {
            return flag;
        }

        public void process(int id, PacketContainer<? extends WrappedPacket<?>> input, List<AppendedData> data) {
            if (isProcessed(id)) {
                return;
            }

            lastProcessId = id;
            this.output = flag.test(input);
            flag.append(input, this, data);
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
        public void toggle(int id) throws GetOutputWithoutProcessException {
            if (!isProcessed(id)) {
                throw new GetOutputWithoutProcessException();
            }

            output = !output;
        }

        @Override
        public Array<MonitorFlag<PacketContainer<? extends WrappedPacket<?>>>> getFlags() {
            return Array.from(flag);
        }

        @Override
        public boolean shouldAppend() {
            return appending;
        }

        @Override
        public void acceptAppend() {
            appending = true;
        }
    }
}
