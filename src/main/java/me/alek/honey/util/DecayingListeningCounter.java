package me.alek.honey.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Supplier;

public class DecayingListeningCounter {

    private final Supplier<Long> collectInterval;
    private final Supplier<Long> max;
    private final Runnable onFlag;

    private final LinkedList<Long> data = new LinkedList<>();

    public DecayingListeningCounter(Supplier<Long> collectInterval, Runnable onFlag, Supplier<Long> max) {
        this.collectInterval = collectInterval;
        this.onFlag = onFlag;
        this.max = max;
    }

    public DecayingListeningCounter(Supplier<Long> collectInterval, Runnable onFlag) {
        this(collectInterval, onFlag, null);
    }

    public DecayingListeningCounter(Supplier<Long> collectInterval) {
        this(collectInterval, null, null);
    }

    private void collect(long time) {
        synchronized (data) {
            final Iterator<Long> sampleNodeIterator = data.iterator();
            while (sampleNodeIterator.hasNext()) {

                final long diff = time - sampleNodeIterator.next();
                if (diff > collectInterval.get()) {
                    sampleNodeIterator.remove();
                    continue;
                }
                return;
            }
        }
    }

    public void count() {
        count(System.currentTimeMillis());
    }

    public void count(long time) {
        collect(time);

        synchronized (data) {
            data.addLast(time);
        }
        if (max != null && data.size() >= max.get()) {
            if (onFlag != null) onFlag.run();

            synchronized (data) {
                data.clear();
            }
        }
    }

    public int size() {
        collect(System.currentTimeMillis());

        return data.size();
    }
}

