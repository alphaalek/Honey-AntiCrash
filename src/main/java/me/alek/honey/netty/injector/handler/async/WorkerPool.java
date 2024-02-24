package me.alek.honey.netty.injector.handler.async;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.Bukkit;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class WorkerPool<T> {

    public static WorkerPool<Object> ASYNC_PACKET_DECODER_POOL =
            new WorkerPool<>(ConfigurationOptions.AsyncDecoder.MIN_WORKER_THREADS, ConfigurationOptions.AsyncDecoder.MIN_TIME_FOR_QUARANTINE);

    private Worker<T>[] workers;
    private final int minimumWorkerThreads;
    private final double minimumTaskTimeForQuarantine;
    private boolean terminated;

    public class TaskWorker implements Worker<T>, Runnable {

        private final LinkedBlockingQueue<ChainedCallable<T>> pendingTasks = new LinkedBlockingQueue<>();
        private ChainedCallable<T> currentTask;
        private long recentTaskStart;
        private boolean quarantined;

        @Override
        public void submit(ChainedCallable<T> task) {
            pendingTasks.add(task);
        }

        @Override
        public Queue<ChainedCallable<T>> getPendingTasks() {
            return pendingTasks;
        }

        @Override
        public boolean shouldRelocate(TaskSupplier<T> supplier) {
            return currentTask != null && currentTask.getFrom() != supplier &&
                    recentTaskStart != 0 && System.currentTimeMillis() - recentTaskStart > minimumTaskTimeForQuarantine;
        }

        @Override
        public boolean isQuarantined() {
            return quarantined;
        }

        @Override
        public ChainedCallable<T> getCurrentTask() {
            return currentTask;
        }

        @Override
        public void setQuarantined() {
            quarantined = true;
        }

        @Override
        public void run() {
            while (!terminated) {
                try {
                    currentTask = pendingTasks.take();
                    recentTaskStart = System.currentTimeMillis();
                    try {
                        currentTask.call();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred when executing task.");
                    }
                    recentTaskStart = 0;
                    quarantined = false;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public Worker<T> submit(TaskSupplier<T> taskSupplier) {
        Worker<T> worker = getWorkerForTask();
        worker.submit(taskSupplier.getTask());
        return worker;
    }

    public synchronized Worker<T> getWorkerForTask() {
        Worker<T> worker = workers[0];

        for (int i = 1; i < workers.length; i++) {
            Worker<T> otherWorker = workers[i];
            if (worker.isQuarantined() ||
                    !otherWorker.isQuarantined() && worker.getPendingTasks().size() + 1 > otherWorker.getPendingTasks().size()) {

                workers[i] = worker;
                workers[i - 1] = otherWorker;
            }
        }
        if (workers[0].isQuarantined()) {
            Worker<T>[] newWorkers = new Worker[workers.length + 1];
            System.arraycopy(workers, 0, newWorkers, 1, workers.length);

            TaskWorker newWorker = new TaskWorker();
            newWorkers[0] = newWorker;
            new Thread(newWorker).start();

            this.workers = newWorkers;
            return newWorker;
        }

        return worker;
    }

    public Worker<T> relocateWorkerTasks(Worker<T> from) {
        Bukkit.broadcastMessage("Relocating from " + from);

        from.setQuarantined();
        Worker<T> worker = getWorkerForTask();
        for (ChainedCallable<T> task : from.getPendingTasks()) {
            worker.submit(task);
        }
        return worker;
    }

    public WorkerPool(int minimumWorkerThreads, double minimumTaskTimeForQuarantine) {
        this.minimumWorkerThreads = minimumWorkerThreads;
        this.minimumTaskTimeForQuarantine = minimumTaskTimeForQuarantine;

        this.workers = new Worker[minimumWorkerThreads];
        for (int i = 0; i < minimumWorkerThreads; i++) {
            TaskWorker worker = new TaskWorker();
            workers[i] = worker;
            new Thread(worker).start();
        }
    }

    public void terminate() {
        terminated = true;
    }
}