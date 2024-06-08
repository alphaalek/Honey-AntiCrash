package me.alek.honey.async.worker;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.async.ChainedCallable;
import me.alek.honey.async.TaskSupplier;
import me.alek.honey.async.Worker;
import me.alek.honey.async.WorkerPool;
import org.bukkit.Bukkit;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class IsolatorWorkerWrapper<T> implements Worker<T> {

    private final LinkedBlockingQueue<ChainedCallable<T>> pendingTasks = new LinkedBlockingQueue<>();
    private ChainedCallable<T> currentTask;
    private long recentTaskStart;
    private int supplierCount;

    private final DelegatedIsolatorWorker<T> worker;

    public IsolatorWorkerWrapper(WorkerPool<T> workerPool, Class<DelegatedIsolatorWorker<T>> worker) {
        try {
            this.worker = worker.getDeclaredConstructor(WorkerPool.class, Worker.class)
                    .newInstance(workerPool, this);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to initialize isolator worker wrapper");
        }
    }

    @Override
    public void run() {
        try {
            currentTask = pendingTasks.take();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        recentTaskStart = System.currentTimeMillis();
        worker.preCall();
        try {
            currentTask.call();
        } catch (Exception ex) {
            ex.printStackTrace();
            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred when executing task.");
        }
        recentTaskStart = 0;
        worker.postCall();
    }

    @Override
    public synchronized boolean relocate(TaskSupplier<T> supplier) {
        return worker.relocate(supplier);
    }

    @Override
    public String name() {
        throw new UnsupportedOperationException("Can't get name from isolator worker!");
    }

    @Override
    public synchronized void submit(TaskSupplier<T> supplier) {
        submit(supplier.getTask());
    }

    @Override
    public void submit(ChainedCallable<T> callable) {
        try {
            if (!pendingTasks.offer(callable, 0, TimeUnit.MILLISECONDS)) {
                Bukkit.getLogger().info("Couldn't offer task to worker");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public synchronized void incrementSupplierCount() {
        supplierCount++;
    }

    @Override
    public synchronized void decrementSupplierCount() {
        supplierCount--;
    }

    @Override
    public int supplierCount() {
        return supplierCount;
    }

    @Override
    public Queue<ChainedCallable<T>> getPendingTasks() {
        return pendingTasks;
    }

    @Override
    public Queue<ChainedCallable<T>> getPendingTasks(TaskSupplier<T> supplier) {
        return pendingTasks.stream()
                .filter(c -> c.getFrom().equals(supplier))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public ChainedCallable<T> getCurrentTask() {
        return currentTask;
    }

    @Override
    public boolean isQuarantined() {
        return worker.shouldQuarantine();
    }

    @Override
    public long recentTaskStart() {
        return recentTaskStart;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException();
    }
}
