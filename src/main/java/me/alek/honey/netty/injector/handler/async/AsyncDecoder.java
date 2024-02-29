package me.alek.honey.netty.injector.handler.async;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class AsyncDecoder implements TaskSupplier<Object> {

    private final HoneyPlayer player;
    private final WorkerPool<Object> workerPool;
    private ChainedCallable<Object> currentTask;
    private Worker<Object> worker;
    private final AtomicLong lastHandledTask = new AtomicLong();

    public AsyncDecoder(HoneyPlayer player, WorkerPool<Object> workerPool) {
        this.player = player;
        this.workerPool = workerPool;
    }

    public HoneyPlayer getPlayer() {
        return player;
    }

    public synchronized void decode(Consumer<Object> consumer) {
        submitTask(new ChainedCallable<>(this, () -> null).addListener(consumer));
    }

    public synchronized void encode(Consumer<Object> consumer) {
        if (currentTask == null || currentTask.isCalled())
            consumer.accept(currentTask);
        else
            currentTask.addListener(consumer);
    }

    @Override
    public void submitTask(ChainedCallable<Object> task) {
        currentTask = task;

        if (lastHandledTask.get() == 0) lastHandledTask.set(System.currentTimeMillis());
        currentTask.addListener((Consumer<Object>) (d) -> lastHandledTask.set(System.currentTimeMillis()));

        if (worker != null)
            worker.submit(currentTask);
        else
            worker = workerPool.submit(this);

        Bukkit.getLogger().info("Worker " + worker + " " + worker.relocate(this) + " " + worker.isQuarantined());

        if (worker.isQuarantined())
            if (worker.relocate(this))
                worker = workerPool.relocateWorkerTasks(this, worker);
            else
                HoneyPlugin.get().getNotificationService().sendActionBar(player, "§cDecoder worker moved to quarantine... (" + (System.currentTimeMillis() - lastHandledTask.get()) + "ms)");
    }

    @Override
    public ChainedCallable<Object> getTask() {
        return currentTask;
    }
}
