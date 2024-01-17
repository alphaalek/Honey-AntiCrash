package me.alek.honey.tracker;

import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class TempQueue {

    private final Queue<Double> FIXED = new LinkedList<>();
    private final Queue<TimedNode> TIMED = new LinkedList<>();

    private final boolean isFixed;
    private Timespan timespan;
    private AtomicDouble total;

    public TempQueue(boolean isFixed) {
        this.isFixed = isFixed;
    }

    public void prepareFixed(Timespan timespan, int amount, AtomicDouble total, double defaultValue) {
        for (int i = 0; i < amount; i++) {
            FIXED.add(defaultValue);
        }
        total.addAndGet(1000 * timespan.getTimespan());
    }

    public void prepareTimed(Timespan timespan, AtomicDouble total) {
        this.timespan = timespan;
        this.total = total;
    }

    public Queue<Double> asFixedQueue() {
        if (!isFixed) throw new UnsupportedOperationException();

        return FIXED;
    }

    public Queue<TimedNode> asTimedQueue() {
        if (isFixed) throw new UnsupportedOperationException();

        return TIMED;
    }

    public double add(double amount) {
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

                    total.addAndGet(-node.value);
                    continue;
                }
                break;
            }
        }
        return 0;
    }

    public static class TimedNode {

        public TimedNode(double value) { this.value = value; }

        private final double value;
        private final long insertionInstant = System.currentTimeMillis();
    }
}
