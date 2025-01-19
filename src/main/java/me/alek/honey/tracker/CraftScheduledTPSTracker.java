package me.alek.honey.tracker;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.compat.spark.SparkTPSProvider;
import me.alek.honey.tracker.temp.TempIntervalTracker;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

public class CraftScheduledTPSTracker extends TempIntervalTracker {

    private final CyclicBarrier barrier = new CyclicBarrier(2);
    private final AtomicLong expectedTick = new AtomicLong();

    public double evaluateTPS(Timespan timespan) {
        return ((double) 20 * timespan.getMilliTimespan()) / timespan.getTotal(this);
    }

    public CraftScheduledTPSTracker() {
        super(false, true);
        setupThreads();
    }

    private void setupThreads() {
        AtomicLong lastTick = new AtomicLong(System.currentTimeMillis());
        BukkitRunnable bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastTick.get() < 95) {
                    return;
                }
                lastTick.set(System.currentTimeMillis());

                try {
                    barrier.await();
                } catch (Exception ex) {
                    return;
                }

                long delta = System.currentTimeMillis() - expectedTick.get();
                int amount = 100 + Math.max(0, (int) delta);

                add(amount);
            }
        };
        bukkitRunnable.runTaskTimer(HoneyPlugin.get().getPlugin(), 0L, 2L);

        new Thread(() -> {
            expectedTick.set(System.currentTimeMillis());
            for (; ; ) {
                if (SparkTPSProvider.SparkValidator.VALIDATOR.find().get()) {
                    bukkitRunnable.cancel();
                    break;
                }

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
