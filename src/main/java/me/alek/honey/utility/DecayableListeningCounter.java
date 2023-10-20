package me.alek.honey.utility;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Supplier;

public class DecayableListeningCounter {

    private final Supplier<Long> collectInterval;
    private final Supplier<Long> max;
    private final Runnable onFlag;

    private final LinkedList<Long> data = new LinkedList<>();

    public DecayableListeningCounter(Supplier<Long> collectInterval, Runnable onFlag, Supplier<Long> max) {
        this.collectInterval = collectInterval;
        this.onFlag = onFlag;
        this.max = max;
    }

    public DecayableListeningCounter(Supplier<Long> collectInterval, Runnable onFlag) {
        this(collectInterval, onFlag, null);
    }

    public DecayableListeningCounter(Supplier<Long> collectInterval) {
        this(collectInterval, null, null);
    }

    private void collect() {
        synchronized (data) {
            final Iterator<Long> sampleNodeIterator = data.iterator();
            while (sampleNodeIterator.hasNext()) {

                final long diff = System.currentTimeMillis() - sampleNodeIterator.next();
                if (diff > collectInterval.get()) {
                    sampleNodeIterator.remove();
                    continue;
                }
                break;
            }
        }
    }

    public void count() {
        collect();

        synchronized (data) {
            data.addLast(System.currentTimeMillis());
        }
        if (max != null && data.size() >= max.get()) {
            if (onFlag != null) onFlag.run();

            synchronized (data) {
                data.clear();
            }
        }
    }

    public int size() {
        collect();

        return data.size();
    }
}

