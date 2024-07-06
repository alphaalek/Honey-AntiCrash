package me.alek.honey.tracker.temp;

import me.alek.honey.tracker.TimedNode;
import me.alek.honey.tracker.Timespan;

import java.io.Serializable;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;

public class UnsyncTempQueue implements Serializable {

    public interface Filter extends Predicate<Long>, Serializable {

    }

    private static final long serialVersionUID = 13688663619933788L;

    private final Queue<Double> FIXED = new LinkedList<>();
    private final Deque<TimedNode<Double>> TIMED = new LinkedList<>();

    private final boolean isFixed;
    private final Filter exceeds;
    private boolean collected;
    private long total;

    public UnsyncTempQueue(Timespan timespan, boolean isFixed) {
        this(diff -> diff > timespan.getNanoTimespan(), isFixed);
    }

    public UnsyncTempQueue(Filter exceeds, boolean isFixed) {
        this.exceeds = exceeds;
        this.isFixed = isFixed;
    }

    public long getTotal() {
        return total;
    }

    public void prepareFixed(Timespan timespan, int amount, double defaultValue) {
        if (!isFixed) throw new UnsupportedOperationException();

        for (int i = 0; i < amount; i++) {
            FIXED.add(defaultValue);
        }
        total += timespan.getTimespan() * 1000L;
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

    public Deque<TimedNode<Double>> asTimedQueue() {
        if (isFixed) throw new UnsupportedOperationException();

        synchronized (this) {
            return TIMED;
        }
    }

    public void add(double amount) {
        if (amount <= 0) {
            return;
        }

        if (isFixed) {
            synchronized (this) {
                FIXED.add(amount);
                total += (long) amount - (long) (double) FIXED.remove();
            }
        }

        if (!isFixed) {
            add(new TimedNode<>(amount));
        }
    }

    public void add(TimedNode<Double> node) {
        if (isFixed) {
            add(node.getValue());
        }

        if (!isFixed) {
            if (node.getValue() > 0) {
                synchronized (this) {
                    TIMED.add(node);
                }
            }

            total += (long) (double) node.getValue();

            if (clearNode(node.getInsertionInstant())) {
                synchronized (this) {
                    TIMED.remove();
                }
            }
        }
    }

    public void clearExceedingNodes(long time) {
        if (isFixed) {
            return;
        }

        synchronized (this) {
            TimedNode<Double> peekedNode = TIMED.peek();
            if (peekedNode == null
                    || !clearNode(peekedNode, time)) {
                return;
            }
            TIMED.remove();

            Iterator<TimedNode<Double>> sampleNodeIterator = TIMED.iterator();
            while (sampleNodeIterator.hasNext()) {
                if (clearNode(sampleNodeIterator.next(), time)) {
                    sampleNodeIterator.remove();
                    continue;
                }
                break;
            }
        }
    }

    public void clearExceedingNodes() {
        clearExceedingNodes(System.nanoTime());
    }

    private boolean clearNode(long time) {
        TimedNode<Double> oldestNode = TIMED.peek();
        if (oldestNode != null) {
            return clearNode(oldestNode, time);
        }
        return false;
    }

    private boolean clearNode(TimedNode<Double> node, long time) {
        if (isFixed) {
            return false;
        }

        final long diff = time - node.getInsertionInstant();

        if (exceeds.test(diff)) {
            total -= (long) (double) node.getValue();
            collected = true;
            return true;
        }

        return false;
    }
}
