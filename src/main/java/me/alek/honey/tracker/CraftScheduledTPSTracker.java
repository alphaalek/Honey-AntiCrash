package me.alek.honey.tracker;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.util.FieldAccessor;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.Reflection;
import me.alek.honey.tracker.temp.TempQueuedTracker;
import org.bukkit.Bukkit;
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
        return Math.min(20, ((double) 20 * 1010 * timespan.getTimespan()) / timespan.getTotal(this));
    }

    public CraftScheduledTPSTracker() {
        super(false, true);
        setupThreads();
    }

    private void setupFirstScheduledTask(BukkitRunnable runnable, Plugin plugin, long delay, long period) {
        BukkitScheduler scheduler = HoneyPlugin.get().getPlugin().getServer().getScheduler();
        AtomicReference<BukkitTask> craftTaskTailReference = Reflection.<AtomicReference<BukkitTask>>getField(
                scheduler.getClass(), "tail", AtomicReference.class).get(scheduler);
        BukkitTask initialTail = craftTaskTailReference.get();

        // create task
        BukkitTask task = scheduler.runTaskTimer(plugin, (Runnable) runnable, delay, period);
        // clean up new tail
        craftTaskTailReference.set(initialTail);
        ReflectStructure<BukkitTask, BukkitTask> taskStructure = ReflectStructureCache.acquireStructure(task.getClass())
                .<BukkitTask>withWeakType(task.getClass());

        taskStructure.withTarget(initialTail)
                .writeField(null,"next", 0,null);

        // set first task to the new one
        FieldAccessor<BukkitTask> headTaskField = Reflection.getFieldAssignableFrom(
                scheduler.getClass(), "head", BukkitTask.class);
        taskStructure.withTarget(headTaskField.get(scheduler))
                .writeField(null,"next", 0,task);
    }

    private void setupThreads() {
        final AtomicLong lastTick = new AtomicLong(System.currentTimeMillis());
        HoneyPlugin.get().getPlugin().getServer().getScheduler().runTaskTimer(HoneyPlugin.get().getPlugin(), new BukkitRunnable() {
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
        }, 0L, 2L);

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
