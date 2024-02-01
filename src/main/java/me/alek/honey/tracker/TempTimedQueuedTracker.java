package me.alek.honey.tracker;

import com.google.common.util.concurrent.AtomicDouble;
import me.alek.honey.utility.Tuple2;

public class TempTimedQueuedTracker extends TempQueuedTracker {

    public void add(double amount) {
        for (Tuple2<AtomicDouble, TempQueue> queue : tickDurationQueues) {
            queue.getT2().add(amount);
            if (amount > 0) queue.getT1().addAndGet(amount);
        }
    }
}
