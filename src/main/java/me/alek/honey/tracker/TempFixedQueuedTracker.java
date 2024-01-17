package me.alek.honey.tracker;

import com.google.common.util.concurrent.AtomicDouble;
import me.alek.honey.utility.Tuple2;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TempFixedQueuedTracker extends TempQueuedTracker {

    public double peek(Timespan timespan) {
        Tuple2<AtomicDouble, TempQueue> queue = getTuple(timespan);
        if (queue == null) return 0;

        return Optional.ofNullable(queue.getT2().asFixedQueue().peek()).orElse(0d);
    }

    public void add(double amount) {
        for (Tuple2<AtomicDouble, TempQueue> queue : tickDurationQueues) {
            double removed = queue.getT2().add(amount);
            queue.getT1().addAndGet(amount - removed);
        }
    }
}
