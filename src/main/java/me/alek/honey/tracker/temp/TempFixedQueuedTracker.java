package me.alek.honey.tracker.temp;

import com.google.common.util.concurrent.AtomicDouble;
import me.alek.honey.tracker.timed.Timespan;
import me.alek.honey.utility.Tuple2;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class TempFixedQueuedTracker extends TempQueuedTracker {

    public TempFixedQueuedTracker(boolean hasId) {
        super(hasId);
    }

    public double peek(Timespan timespan) {
        Tuple2<AtomicLong, TempQueue> queue = getTuple(timespan);
        if (queue == null) return 0;

        return Optional.ofNullable(queue.getT2().asFixedQueue().peek()).orElse(0d);
    }

    public void add(double amount) {
        for (Tuple2<AtomicLong, TempQueue> queue : tickDurationQueues) {
            double removed = queue.getT2().add(amount);
            queue.getT1().addAndGet((long) (amount - removed));
        }
    }
}
