package me.alek.honey.tracker.temp;

import me.alek.honey.tracker.TimedNode;
import me.alek.honey.utility.Tuple2;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicLong;

public class TempTimedQueuedTracker extends TempQueuedTracker {

    public TempTimedQueuedTracker(boolean hasId) {
        super(hasId);
    }

    public void add(double amount) {
        add(new TimedNode<>(amount));
    }

    public void add(TimedNode<Double> node) {
        for (Tuple2<AtomicLong, TempQueue> queue : tickDurationQueues) {
            queue.getT2().add(node);
            if (node.getValue() > 0) {
                queue.getT1().addAndGet((long) (double) node.getValue());
            }
        }
    }
}
