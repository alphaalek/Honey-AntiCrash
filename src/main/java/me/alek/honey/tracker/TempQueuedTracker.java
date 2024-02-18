package me.alek.honey.tracker;

import com.google.common.util.concurrent.AtomicDouble;
import me.alek.honey.utility.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class TempQueuedTracker implements HoneyTracker {

    protected final List<Tuple2<AtomicDouble, TempQueue>> tickDurationQueues = new ArrayList<>();

    public TempQueuedTracker() {
        setup();
    }

    private synchronized void setup() {
        for (Timespan timespan : Timespan.values()) {
            timespan.clearContext(this);
            tickDurationQueues.add(new Tuple2<>(timespan.getTotal(this), timespan.getQueue(this)));
        }
    }

    public void clearExceedingNodes(Timespan timespan, long time) {
        Tuple2<AtomicDouble, TempQueue> queue = getTuple(timespan);
        if (queue == null) return;

        queue.getT2().clearExceedingNodes(time);
    }

    public double getTotal(Timespan timespan) {
        Tuple2<AtomicDouble, TempQueue> queue = getTuple(timespan);
        if (queue == null) return 0;

        return queue.getT1().get();
    }

    public TempQueue getQueue(Timespan timespan) {
        Tuple2<AtomicDouble, TempQueue> queue = getTuple(timespan);
        if (queue == null) return null;

        return queue.getT2();
    }

    public synchronized Tuple2<AtomicDouble, TempQueue> getTuple(Timespan timespan) {
        for (Tuple2<AtomicDouble, TempQueue> queue : tickDurationQueues) {
            if (timespan.getQueue(this).equals(queue.getT2()))
                return queue;
        }
        setup();

        return getTuple(timespan);
    }

    public synchronized int getLength(Timespan timespan) {
        for (TempQueue queue : tickDurationQueues.stream().map(Tuple2::getT2).collect(Collectors.toSet())) {
            if (queue.getTimespan() == timespan)
                return queue.getQueueSize();
        }
        setup();

        return getLength(timespan);
    }
}
