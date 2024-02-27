package me.alek.honey.netty.injector.handler.async.worker;

import me.alek.honey.netty.injector.handler.async.ChainedCallable;
import me.alek.honey.netty.injector.handler.async.TaskSupplier;
import me.alek.honey.netty.injector.handler.async.Worker;
import me.alek.honey.netty.injector.handler.async.WorkerPool;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public class HighPendingIsolatorWorker<T> implements DelegatedIsolatorWorker<T> {

    private final WorkerPool<T> workerPool;
    private final Worker<T> worker;

    public HighPendingIsolatorWorker(WorkerPool<T> workerPool, Worker<T> worker) {
        this.workerPool = workerPool;
        this.worker = worker;
    }

    @Override
    public boolean isQuarantined() {
        return worker.getPendingTasks().size() > workerPool.getMinimumPendingTaskSizeForQuarantine();
    }

    private TaskSupplier<T> getHighestPendingSupplier() {
        if (worker.getPendingTasks().isEmpty()) return null;

        Map<TaskSupplier<T>, Integer> taskOriginMap = new HashMap<>();
        for (ChainedCallable<T> task : worker.getPendingTasks()) {
            taskOriginMap.put(task.getFrom(), taskOriginMap.getOrDefault(task.getFrom(), 0) + 1);
        }
        Map.Entry<TaskSupplier<T>, Integer> max = null;
        for (Map.Entry<TaskSupplier<T>, Integer> taskOrigin : taskOriginMap.entrySet()) {
            if (max == null || taskOrigin.getValue() > max.getValue())
                max = taskOrigin;
        }
        return max.getKey();
    }

    @Override
    public boolean relocate(TaskSupplier<T> supplier) {
        TaskSupplier<T> highestSupplier = getHighestPendingSupplier();
        if (highestSupplier == null) return false;

        return !highestSupplier.equals(supplier);
    }
}
