package me.alek.honey.utility;

import me.alek.honey.tracker.TimedNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SampleDecayingList<T> {

    private final long collectInterval;
    private final int sampleSize;
    private final int lockAmount;
    private final LinkedList<TimedNode<T>> sampleNodes = new LinkedList<>();
    private final LinkedList<T> lockedNodes = new LinkedList<>();

    public SampleDecayingList(long collectInterval, int sampleSize, int lockAmount) {
        this.collectInterval = collectInterval;
        this.sampleSize = sampleSize;
        this.lockAmount = lockAmount;
    }

    public void put(T node) {
        synchronized (sampleNodes) {
            sampleNodes.addLast(new TimedNode<>(node));

            if (sampleNodes.size() > sampleSize) sampleNodes.removeFirst();

            if (lockAmount > 0) {
                lockedNodes.addLast(node);
                if (lockedNodes.size() > lockAmount) lockedNodes.removeFirst();
            }
        }
    }

    private void collect() {
        synchronized (sampleNodes) {

            final Iterator<TimedNode<T>> sampleNodeIterator = sampleNodes.iterator();
            while (sampleNodeIterator.hasNext()) {

                final TimedNode<T> node = sampleNodeIterator.next();
                final long diff = System.currentTimeMillis() - node.getInsertionInstant();

                if (diff > collectInterval) {
                    sampleNodeIterator.remove();
                    continue;
                }
                break;
            }
        }
    }

    private T getWithoutCollecting(int index) {
        synchronized (sampleNodes) {
            return sampleNodes.get(index).getValue();
        }
    }

    public T get(int index) {
        collect();
        synchronized (sampleNodes) {
            return sampleNodes.get(index).getValue();
        }
    }

    public T getLast() {
        return get(sampleNodes.size());
    }

    public T getFirst() {
        return get(0);
    }

    public List<T> getAll() {
        collect();
        final List<T> list = new ArrayList<>();

        for (int i = 0; i < sampleNodes.size(); i++) {
            list.add(getWithoutCollecting(i));
        }

        return list;
    }

    public List<T> getLocked() {
        return lockedNodes;
    }
}
