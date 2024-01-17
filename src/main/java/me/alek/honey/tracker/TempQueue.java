package me.alek.honey.tracker;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class TempQueue {

    private final Queue<Integer> FIXED = new LinkedList<>();
    private final Queue<TimedNode> TIMED = new LinkedList<>();

    private final boolean isFixed;
    private Timespan timespan;
    private AtomicInteger total;
    private Supplier<AtomicInteger> totalSupplier;

    public TempQueue(boolean isFixed) {
        this.isFixed = isFixed;
    }

    public void prepareFixed(int amount, int defaultValue) {
        for (int i = 0; i < amount; i++) {
            FIXED.add(defaultValue);
        }
    }

    public void prepareTimed(Timespan timespan, Supplier<AtomicInteger> total) {
        this.timespan = timespan;
        this.totalSupplier = total;
    }

    public Queue<Integer> asFixedQueue() {
        if (!isFixed) throw new UnsupportedOperationException();

        return FIXED;
    }

    public Queue<TimedNode> asTimedQueue() {
        if (isFixed) throw new UnsupportedOperationException();

        return TIMED;
    }



    public int add(int amount) {
        if (isFixed) {
            FIXED.add(amount);
            return FIXED.remove();
        } else {
            TIMED.add(new TimedNode(amount));

            final Iterator<TimedNode> sampleNodeIterator = TIMED.iterator();
            while (sampleNodeIterator.hasNext()) {

                final TimedNode node = sampleNodeIterator.next();
                final long diff = System.currentTimeMillis() - node.insertionInstant;

                if (diff > ((long)timespan.getTimespan()) * 1000) {
                    sampleNodeIterator.remove();

                    if (total == null) total = totalSupplier.get();
                    total.addAndGet(-node.value);

                    continue;
                }
                break;
            }
        }
        return 0;
    }

    public class TimedNode {

        public TimedNode(int value) { this.value = value; }

        private final int value;
        private final long insertionInstant = System.currentTimeMillis();
    }
}
