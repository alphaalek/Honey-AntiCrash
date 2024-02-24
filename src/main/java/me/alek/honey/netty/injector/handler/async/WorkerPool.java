package me.alek.honey.netty.injector.handler.async;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.utility.PredicateUtils;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WorkerPool<T> {

    public static WorkerPool<Object> ASYNC_PACKET_DECODER_POOL =
            new WorkerPool<>(ConfigurationOptions.AsyncDecoder.MIN_WORKER_THREADS, ConfigurationOptions.AsyncDecoder.MIN_TIME_FOR_QUARANTINE);

    private Worker<T>[] workers;
    private final List<Worker<T>> allWorkers = new ArrayList<>();
    private final int minimumWorkerThreads;
    private final double minimumTaskTimeForQuarantine;

    public class TaskWorker implements Worker<T>, Runnable {

        private final LinkedBlockingQueue<ChainedCallable<T>> pendingTasks = new LinkedBlockingQueue<>();
        private ChainedCallable<T> currentTask;
        private long recentTaskStart;
        private boolean quarantined;
        private boolean running = true;

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
            if (shouldQuarantine()) {
                quarantined = true;
                return currentTask != null && currentTask.getFrom() != supplier;
            }
            return false;
        }

        @Override
        public boolean shouldQuarantine() {
            return recentTaskStart != 0 && System.currentTimeMillis() - recentTaskStart > minimumTaskTimeForQuarantine;
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
        public boolean isActive() {
            return recentTaskStart != 0;
        }

        @Override
        public void destroy() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
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
            synchronized (this) {
                allWorkers.remove(this);
            }
        }
    }

    public Worker<T> submit(TaskSupplier<T> taskSupplier) {
        Worker<T> worker = getWorkerForTask();
        worker.submit(taskSupplier.getTask());
        return worker;
    }

    public synchronized Worker<T> getWorkerForTask() {
        Worker<T> headWorker = workers[0];

        for (int i = 1; i < workers.length; i++) {
            Worker<T> otherWorker = workers[i];
            if (headWorker.isQuarantined() ||
                    !otherWorker.isQuarantined() && headWorker.getPendingTasks().size() + 1 > otherWorker.getPendingTasks().size()) {

                workers[i] = headWorker;
                workers[i - 1] = otherWorker;
            }
        }
        Bukkit.getLogger().info("Getting worker " + workers.length + " " + headWorker + " " + workers[workers.length - 1].isQuarantined());
        if (workers[0].isQuarantined() || workers[0].getPendingTasks().size() > 100) {
            return createNewTaskWorker();
        }

        return workers[0];
    }
    
    public Worker<T> createNewTaskWorker() {
        Worker<T>[] newWorkers = new Worker[workers.length + 1];
        System.arraycopy(workers, 0, newWorkers, 1, workers.length);

        Worker<T> newWorker = initNewTaskWorker(newWorkers, newWorkers.length);

        this.workers = newWorkers;
        return newWorker;
    }

    private Worker<T> initNewTaskWorker(Worker<T>[] newWorkers, int indexName) {
        TaskWorker newWorker = new TaskWorker();
        newWorkers[0] = newWorker;
        allWorkers.add(newWorker);

        new Thread(newWorker, getTaskWorkerThreadName(indexName)).start();

        return newWorker;
    }

    public synchronized void garbageCollect() {
        List<Worker<T>> pressuredWorkers = Arrays.stream(workers).filter(PredicateUtils.or(
                Worker::isQuarantined,
                Worker::shouldQuarantine,
                worker -> worker.getPendingTasks().size() > 100)
        ).collect(Collectors.toList());

        if (workers.length - pressuredWorkers.size() < minimumWorkerThreads) return;

        Worker<T>[] newWorkers = new Worker[minimumWorkerThreads];
        int i = 0;
        for (Worker<T> worker : workers) {
            if (!pressuredWorkers.contains(worker)) newWorkers[i++] = worker;
        }
        workers = newWorkers;

        List<Worker<T>> availableWorkers = Arrays.asList(workers);
        for (Worker<T> worker : allWorkers) {
            if (!availableWorkers.contains(worker) && worker.getPendingTasks().size() < 5) {
                worker.destroy();
                relocateWorkerTasks(worker);
            }
        }
    }

    public Worker<T> relocateWorkerTasks(Worker<T> from) {
        from.setQuarantined();
        Worker<T> worker = getWorkerForTask();
        for (ChainedCallable<T> task : from.getPendingTasks()) {
            worker.submit(task);
        }
        return worker;
    }

    public String getTaskWorkerThreadName(int indexName) {
        return "packet-decoder-" + indexName + "-thread";
    }

    public WorkerPool(int minimumWorkerThreads, double minimumTaskTimeForQuarantine) {
        this.minimumWorkerThreads = minimumWorkerThreads;
        this.minimumTaskTimeForQuarantine = minimumTaskTimeForQuarantine;

        this.workers = new Worker[minimumWorkerThreads];
        for (int i = 0; i < minimumWorkerThreads; i++) {
            initNewTaskWorker(workers, i + 1);
        }

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::garbageCollect, 0, 30L, TimeUnit.SECONDS);
    }

    public void terminate() {
        for (Worker<T> worker : workers) {
            worker.destroy();
        }
    }
}