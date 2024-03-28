package me.alek.honey.tracker.temp;

import me.alek.honey.tracker.TimedNode;
import me.alek.honey.tracker.Timespan;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class TempQueue {

    private final Queue<Double> FIXED = new LinkedList<>();
    private final Queue<TimedNode<Double>> TIMED = new LinkedList<>();

    private final boolean isFixed;
    private boolean collected;
    private Timespan timespan;
    private AtomicLong total;

    public TempQueue(boolean isFixed) {
        this.isFixed = isFixed;
    }

    public Timespan getTimespan() {
        return timespan;
    }

    public void prepareFixed(Timespan timespan, int amount, AtomicLong total, double defaultValue) {
        if (!isFixed) throw new UnsupportedOperationException();

        for (int i = 0; i < amount; i++) {
            FIXED.add(defaultValue);
        }
        total.addAndGet(timespan.getTimespan() * 1000L);
    }

    public void prepareTimed(Timespan timespan, AtomicLong total) {
        if (isFixed) throw new UnsupportedOperationException();

        this.timespan = timespan;
        this.total = total;
    }

    public boolean isCollected() {
        return collected;
    }

    public int getQueueSize() {
        return isFixed ? FIXED.size() : TIMED.size();
    }

    public Queue<Double> asFixedQueue() {
        if (!isFixed) throw new UnsupportedOperationException();

        return FIXED;
    }

    public Queue<TimedNode<Double>> asTimedQueue() {
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
                if (amount > 0) TIMED.add(new TimedNode<>(amount));

                // clearExceedingNodes(time);
            }
        }
        return 0;
    }

    public synchronized void clearExceedingNodes(long time) {
        if (isFixed) return;

        TimedNode<Double> peekedNode = TIMED.peek();
        if (peekedNode == null || !clearNode(peekedNode, time)) {
            return;
        }
        TIMED.remove();

        final Iterator<TimedNode<Double>> sampleNodeIterator = TIMED.iterator();
        while (sampleNodeIterator.hasNext()) {

            if (clearNode(sampleNodeIterator.next(), time)) {
                sampleNodeIterator.remove();
                continue;
            }
            break;
        }
    }

    private boolean clearNode(TimedNode<Double> node, long time) {
        final long diff = time - node.getInsertionInstant();

        if (diff > timespan.getNanoTimespan()) {
            total.addAndGet((long) -node.getValue());
            collected = true;
            return true;
        }
        return false;
    }

    public void clearExceedingNodes() {
        clearExceedingNodes(System.nanoTime());
    }
}
