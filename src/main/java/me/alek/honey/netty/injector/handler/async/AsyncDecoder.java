package me.alek.honey.netty.injector.handler.async;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class AsyncDecoder implements TaskSupplier<Object> {

    private final HoneyPlayer player;
    private final Queue<ChainedCallable<Object>> eventTriggerQueue = new LinkedList<>();
    private final WorkerPool<Object> workerPool;
    private Worker<Object> worker;

    public AsyncDecoder(HoneyPlayer player, WorkerPool<Object> workerPool) {
        this.player = player;
        this.workerPool = workerPool;
    }

    public void decode(Consumer<Object> consumer) {
        submitTask(new ChainedCallable<>(this, () -> null).addListener(consumer));
    }

    @Override
    public void submitTask(ChainedCallable<Object> task) {
        eventTriggerQueue.add(task);

        if (worker != null)
            worker.submit(getTask());
        else
            worker = workerPool.submit(this);

        if (worker.shouldRelocate(this)) {
            worker = workerPool.relocateWorkerTasks(worker);
        }
        if (worker.isQuarantined() && worker.getCurrentTask().getFrom().equals(this)) {
            HoneyPlugin.get().getNotificationService().sendActionBarPacket1_8(player, "§cDecoder worker moved to quarantine...");
        }
    }

    @Override
    public ChainedCallable<Object> getTask() {
        return eventTriggerQueue.poll();
    }
}
