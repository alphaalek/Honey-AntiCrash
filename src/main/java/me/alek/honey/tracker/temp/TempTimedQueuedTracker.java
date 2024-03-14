package me.alek.honey.tracker.temp;

import com.google.common.util.concurrent.AtomicDouble;
import me.alek.honey.tracker.temp.TempQueue;
import me.alek.honey.tracker.temp.TempQueuedTracker;
import me.alek.honey.utility.Tuple2;

import java.util.concurrent.atomic.AtomicLong;

public class TempTimedQueuedTracker extends TempQueuedTracker {

    public TempTimedQueuedTracker(boolean hasId) {
        super(hasId);
    }

    public void add(double amount, long time) {
        for (Tuple2<AtomicLong, TempQueue> queue : tickDurationQueues) {
            queue.getT2().add(amount, time);
            if (amount > 0) queue.getT1().addAndGet((long) amount);
        }
    }

    public void add(double amount) {
        add(amount, System.currentTimeMillis());
    }
}
