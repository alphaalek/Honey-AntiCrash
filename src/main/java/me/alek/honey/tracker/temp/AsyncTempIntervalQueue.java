package me.alek.honey.tracker.temp;

import me.alek.honey.tracker.TimedNode;
import me.alek.honey.tracker.Timespan;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;

public class AsyncTempIntervalQueue {

    private final Queue<Long> FIXED = new LinkedList<>();
    private final Deque<TimedNode<Long>> TIMED = new LinkedList<>();

    private final boolean isFixed;
    private final Predicate<Long> exceeds;
    private boolean collected;
    private long total;

    public AsyncTempIntervalQueue(Timespan timespan, boolean isFixed) {
        this(diff -> diff > timespan.getNanoTimespan(), isFixed);
    }

    public AsyncTempIntervalQueue(Predicate<Long> exceeds, boolean isFixed) {
        this.exceeds = exceeds;
        this.isFixed = isFixed;
    }

    public long getTotal() {
        return total;
    }

    public void prepareFixed(Timespan timespan, int amount, long defaultValue) {
        if (!isFixed) {
            throw new UnsupportedOperationException();
        }

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

    public Queue<Long> asFixedQueue() {
        if (!isFixed) {
            throw new UnsupportedOperationException();
        }

        return FIXED;
    }

    public Deque<TimedNode<Long>> asTimedQueue() {
        if (isFixed) {
            throw new UnsupportedOperationException();
        }

        synchronized (this) {
            return TIMED;
        }
    }

    public void add(long amount) {
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

    public void add(TimedNode<Long> node) {
        if (isFixed) {
            add(node.getValue());
        }

        if (!isFixed) {
            synchronized (this) {
                if (node.getValue() > 0) {
                    TIMED.add(node);
                }

                total += node.getValue();

                if (clearNode(node.getInsertionInstant())) {
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
            TimedNode<Long> peekedNode = TIMED.peek();
            if (peekedNode == null
                    || !clearNode(peekedNode, time)) {
                return;
            }
            TIMED.remove();

            Iterator<TimedNode<Long>> sampleNodeIterator = TIMED.iterator();
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
        TimedNode<Long> oldestNode = TIMED.peek();
        if (oldestNode != null) {
            return clearNode(oldestNode, time);
        }
        return false;
    }

    private boolean clearNode(TimedNode<Long> node, long time) {
        if (isFixed) {
            return false;
        }

        final long diff = time - node.getInsertionInstant();
        if (exceeds.test(diff)) {
            total -= node.getValue();
            collected = true;
            return true;
        }

        return false;
    }
}
