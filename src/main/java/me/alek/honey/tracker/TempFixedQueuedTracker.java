package me.alek.honey.tracker;

import me.alek.honey.utility.Tuple2;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TempFixedQueuedTracker extends TempQueuedTracker {

    public int peek(Timespan timespan) {
        Tuple2<AtomicInteger, TempQueue> queue = getTuple(timespan);
        if (queue == null) return 0;

        return Optional.ofNullable(queue.getT2().asFixedQueue().peek()).orElse(0);
    }

    public void add(int amount) {
        for (Tuple2<AtomicInteger, TempQueue> queue : tickDurationQueues) {
            int removed = queue.getT2().add(amount);
            queue.getT1().addAndGet(amount - removed);
        }
    }
}
