package me.alek.honey.tracker;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.tracker.temp.TempFixedQueuedTracker;
import me.alek.honey.tracker.timed.Timespan;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

public class Tickloop extends TempFixedQueuedTracker {

    private final CyclicBarrier barrier = new CyclicBarrier(2);
    private final AtomicLong expectedTick = new AtomicLong();

    public double evaluateTPS(Timespan timespan) {
        return Math.min(20, 20 * 1005 * timespan.getTimespan() / timespan.getTotal(this).get());
    }

    public Tickloop() {
        super();
        new BukkitRunnable() {
            @Override
            public void run() {
                setupThreads();
            }
        }.runTaskLater(HoneyPlugin.get().getPlugin(), 100L);
    }

    private void setupThreads() {
        final AtomicLong lastTick = new AtomicLong(System.currentTimeMillis());
        new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastTick.get() < 95) return;
                lastTick.set(System.currentTimeMillis());

                try {
                    barrier.await();
                } catch (Exception ex) {
                    return;
                }
                long delta = System.currentTimeMillis() - expectedTick.get();
                int amount;
                if (delta > 0) amount = 100 + (int) delta;
                else amount = 100;

                add(amount);
            }
        }.runTaskTimer(HoneyPlugin.get().getPlugin(), 0L, 2L);

        new Thread(() -> {
            expectedTick.set(System.currentTimeMillis());
            for (;;) {
                expectedTick.addAndGet(100);

                try {
                    barrier.await();
                } catch (Exception ex) {
                }
                expectedTick.set(System.currentTimeMillis());
            }
        }).start();
    }
}
