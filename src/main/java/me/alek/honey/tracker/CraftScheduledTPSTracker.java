package me.alek.honey.tracker;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.FieldAccessor;
import me.alek.honey.netty.util.Reflection;
import me.alek.honey.tracker.temp.TempQueuedTracker;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CraftScheduledTPSTracker extends TempQueuedTracker {

    private final CyclicBarrier barrier = new CyclicBarrier(2);
    private final AtomicLong expectedTick = new AtomicLong();

    public double evaluateTPS(Timespan timespan) {
        return Math.min(20, (long) 20 * 1005 * timespan.getTimespan() / timespan.getTotal(this));
    }

    public CraftScheduledTPSTracker() {
        super(false, true);
        setupThreads();
    }

    private void setupFirstScheduledTask(BukkitRunnable runnable, Plugin plugin, long delay, long period) {
        BukkitScheduler scheduler = HoneyPlugin.get().getPlugin().getServer().getScheduler();
        ReflectStructure<BukkitTask, BukkitTask> craftTaskStructure;
        AtomicReference<BukkitTask> craftTaskTailReference = Reflection.<AtomicReference<BukkitTask>>getField(
                scheduler.getClass(), "tail").get(scheduler);
        BukkitTask initialTail = craftTaskTailReference.get();

        // create task
        BukkitTask task = scheduler.runTaskTimer(plugin, (Runnable) runnable, delay, period);
        // clean up new tail
        craftTaskTailReference.set(initialTail);
        (craftTaskStructure = ReflectStructureCache.acquireStructure(task.getClass())
                .<BukkitTask>withWeakType(task.getClass()))
                .withTarget(initialTail)
                .writeField(null,"next", 0,null);

        // set head to new task
        FieldAccessor<BukkitTask> headTaskField = Reflection.getField(scheduler.getClass(), "head");
        BukkitTask initialHead = headTaskField.get(scheduler);
        headTaskField.set(scheduler, task);
        craftTaskStructure
                .withTarget(task)
                .writeField(0, initialHead);
    }

    private void setupThreads() {
        final AtomicLong lastTick = new AtomicLong(System.currentTimeMillis());
        setupFirstScheduledTask(new BukkitRunnable() {
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
        }, HoneyPlugin.get().getPlugin(), 0L, 2L);

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
