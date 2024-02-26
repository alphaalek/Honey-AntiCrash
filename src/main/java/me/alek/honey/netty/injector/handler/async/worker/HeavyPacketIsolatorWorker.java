package me.alek.honey.netty.injector.handler.async.worker;

import me.alek.honey.netty.injector.handler.async.TaskSupplier;
import me.alek.honey.netty.injector.handler.async.Worker;
import me.alek.honey.netty.injector.handler.async.WorkerPool;

public class HeavyPacketIsolatorWorker<T> implements DelegatedIsolatorWorker<T> {

    private final WorkerPool<T> workerPool;
    private final Worker<T> worker;

    public HeavyPacketIsolatorWorker(WorkerPool<T> workerPool, Worker<T> worker) {
        this.workerPool = workerPool;
        this.worker = worker;
    }

    @Override
    public boolean isQuarantined() {
        return worker.recentTaskStart() != 0 && System.currentTimeMillis() - worker.recentTaskStart() > workerPool.getMinimumTaskTimeForQuarantine();
    }

    @Override
    public boolean relocate(TaskSupplier<T> supplier) {
        return worker.getCurrentTask() != null && !worker.getCurrentTask().getFrom().equals(supplier);
    }
}
