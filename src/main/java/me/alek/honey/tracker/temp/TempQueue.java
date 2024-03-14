package me.alek.honey.tracker.temp;

import com.google.common.util.concurrent.AtomicDouble;
import me.alek.honey.tracker.timed.Timespan;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class TempQueue {

    private final Queue<Double> FIXED = new LinkedList<>();
    private final Queue<TimedNode> TIMED = new LinkedList<>();

    private final boolean isFixed;
    private Timespan timespan;
    private AtomicDouble total;

    public TempQueue(boolean isFixed) {
        this.isFixed = isFixed;
    }

    public Timespan getTimespan() {
        return timespan;
    }

    public void prepareFixed(Timespan timespan, int amount, AtomicDouble total, double defaultValue) {
        if (!isFixed) throw new UnsupportedOperationException();

        for (int i = 0; i < amount; i++) {
            FIXED.add(defaultValue);
        }
        total.addAndGet(timespan.getNanoTimespan());
    }

    public void prepareTimed(Timespan timespan, AtomicDouble total) {
        if (isFixed) throw new UnsupportedOperationException();

        this.timespan = timespan;
        this.total = total;
    }

    public int getQueueSize() {
        return isFixed ? FIXED.size() : TIMED.size();
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
        if (isFixed) return add(amount, 0);

        return add(amount, System.nanoTime());
    }

    public double add(double amount, long time) {
        if (isFixed && amount > 0) {
            FIXED.add(amount);
            return FIXED.remove();

        } else if (!isFixed) {
            synchronized (this) {
                if (amount > 0) TIMED.add(new TimedNode(amount));

                clearExceedingNodes(time);
            }
        }
        return 0;
    }

    public synchronized void clearExceedingNodes(long time) {
        if (isFixed) return;

        final Iterator<TimedNode> sampleNodeIterator = TIMED.iterator();
        while (sampleNodeIterator.hasNext()) {

            final TimedNode node = sampleNodeIterator.next();
            final long diff = time - node.insertionInstant;

            if (diff > (timespan.getNanoTimespan())) {
                sampleNodeIterator.remove();

                total.addAndGet(-node.value);
                continue;
            }
            break;
        }
    }

    public void clearExceedingNodes() {
        clearExceedingNodes(System.nanoTime());
    }

    public static class TimedNode {

        public TimedNode(double value) { this.value = value; }

        private final double value;
        private final long insertionInstant = System.nanoTime();

        public double getValue() {
            return value;
        }

        public long getInsertionInstant() {
            return insertionInstant;
        }
    }
}
