package me.alek.honey.tracker;

import me.alek.honey.utility.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TempQueuedTracker implements HoneyTracker {

    protected final List<Tuple2<AtomicInteger, TempQueue>> tickDurationQueues = new ArrayList<>();

    public TempQueuedTracker() {
        for (Timespan timespan : Timespan.values()) {
            tickDurationQueues.add(new Tuple2<>(timespan.getTotal(this), timespan.getQueue(this)));
        }
    }

    public int getTotal(Timespan timespan) {
        Tuple2<AtomicInteger, TempQueue> queue = getTuple(timespan);
        if (queue == null) return 0;

        return queue.getT1().get();
    }

    public Tuple2<AtomicInteger, TempQueue> getTuple(Timespan timespan) {
        for (Tuple2<AtomicInteger, TempQueue> queue : tickDurationQueues) {
            if (timespan.getQueue(this).equals(queue.getT2())) {
                return queue;
            }
        }
        return null;
    }
}
