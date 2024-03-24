package me.alek.honey.netty.injector.handler.async.worker;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.injector.handler.async.*;
import org.bukkit.Bukkit;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class IsolatorWorkerWrapper<T> implements RunnableWorker<T> {

    private final LinkedBlockingQueue<ChainedCallable<T>> pendingTasks = new LinkedBlockingQueue<>();
    private ChainedCallable<T> currentTask;
    private long recentTaskStart;

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
    public void run() throws Exception {
        currentTask = pendingTasks.take();
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
    public synchronized void submit(ChainedCallable<T> task) {
        try {
            if (!pendingTasks.offer(task, 0, TimeUnit.MILLISECONDS)) {
                Bukkit.getLogger().info("Couldn't offer task to worker");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        return worker.isQuarantined();
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
