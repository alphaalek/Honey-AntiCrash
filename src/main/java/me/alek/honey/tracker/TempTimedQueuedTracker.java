package me.alek.honey.tracker;

import me.alek.honey.utility.Tuple2;

import java.util.concurrent.atomic.AtomicInteger;

public class TempTimedQueuedTracker extends TempQueuedTracker {

    public void add(int amount) {
        for (Tuple2<AtomicInteger, TempQueue> queue : tickDurationQueues) {
            queue.getT2().add(amount);
            queue.getT1().addAndGet(amount);
        }
    }
}
