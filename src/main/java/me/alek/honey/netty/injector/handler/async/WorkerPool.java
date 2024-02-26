package me.alek.honey.netty.injector.handler.async;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.injector.handler.async.worker.*;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class WorkerPool<T> {

    public static Class<DelegatedIsolatorWorker<?>>[] WORKERS_CLASSES = new Class[]{
            HeavyPacketIsolatorWorker.class,
            HighPendingIsolatorWorker.class};
    public static WorkerPool<Object> ASYNC_PACKET_DECODER_POOL =
            new WorkerPool<>(
                    ConfigurationOptions.AsyncDecoder.MIN_WORKER_THREADS,
                    ConfigurationOptions.AsyncDecoder.MIN_TIME_FOR_QUARANTINE,
                    ConfigurationOptions.AsyncDecoder.MIN_PENDING_TASK_FOR_QUARANTINE);

    private Worker<T>[] workers;
    private final List<Worker<T>> allWorkers = new ArrayList<>();
    private final int minimumWorkerThreads;
    private final int minimumPendingTaskSizeForQuarantine;
    private final double minimumTaskTimeForQuarantine;

    public double getMinimumTaskTimeForQuarantine() {
        return minimumTaskTimeForQuarantine;
    }

    public int getMinimumPendingTaskSizeForQuarantine() {
        return minimumPendingTaskSizeForQuarantine;
    }

    public int getMinimumWorkerThreads() {
        return minimumWorkerThreads;
    }

    private List<RunnableWorker<T>> initializeDelegatedWorkers() {
        List<RunnableWorker<T>> workers = new ArrayList<>();
        for (Class<DelegatedIsolatorWorker<?>> workerClass : WORKERS_CLASSES) {
            try {
                @SuppressWarnings("unchecked")
                RunnableWorker<T> workerWrapper = IsolatorWorkerWrapper.class.getDeclaredConstructor(WorkerPool.class, Class.class)
                        .newInstance(this, workerClass);

                workers.add(workerWrapper);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return workers;
    }

    public class TaskWorker implements Worker<T>, Runnable {

        private boolean running = true;

        private final List<RunnableWorker<T>> workers = initializeDelegatedWorkers();
        private RunnableWorker<T> worker = workers.get(0);

        @Override
        public void run() {
            while (running) {
                try {
                    worker.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            synchronized (WorkerPool.this) {
                allWorkers.remove(this);
            }
        }

        @Override
        public boolean relocate(TaskSupplier<T> supplier) {
            for (RunnableWorker<T> worker : workers) {
                if (worker.relocate(supplier)) {
                    if (!isQuarantined()) this.worker = worker;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void submit(ChainedCallable<T> task) {
            worker.submit(task);
        }

        @Override
        public Queue<ChainedCallable<T>> getPendingTasks() {
            return worker.getPendingTasks();
        }

        @Override
        public Queue<ChainedCallable<T>> getPendingTasks(TaskSupplier<T> supplier) {
            return worker.getPendingTasks(supplier);
        }

        @Override
        public ChainedCallable<T> getCurrentTask() {
            return worker.getCurrentTask();
        }

        @Override
        public boolean isQuarantined() {
            return worker.isQuarantined();
        }

        @Override
        public long recentTaskStart() {
            return worker.recentTaskStart();
        }

        @Override
        public void destroy() {
            running = false;
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
        if (workers[0].isQuarantined()) {
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
        List<Worker<T>> pressuredWorkers = Arrays.stream(workers)
                .filter(Worker::isQuarantined)
                .collect(Collectors.toList());

        if (workers.length - pressuredWorkers.size() < minimumWorkerThreads) return;

        Worker<T>[] newWorkers = new Worker[minimumWorkerThreads];
        int i = 0;
        for (Worker<T> worker : workers) {
            if (!pressuredWorkers.contains(worker)) newWorkers[i++] = worker;
            if (i == minimumWorkerThreads) break;
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
        return relocateWorkerTasks(from.getPendingTasks());
    }

    public Worker<T> relocateWorkerTasks(TaskSupplier<T> supplier, Worker<T> from) {
        return relocateWorkerTasks(from.getPendingTasks(supplier));
    }

    private Worker<T> relocateWorkerTasks(Queue<ChainedCallable<T>> tasks) {
        Worker<T> worker = getWorkerForTask();
        for (ChainedCallable<T> task : tasks) {
            worker.submit(task);
        }
        return worker;
    }

    public String getTaskWorkerThreadName(int indexName) {
        return "packet-decoder-" + indexName + "-thread";
    }

    public WorkerPool(int minimumWorkerThreads, double minimumTaskTimeForQuarantine, int minimumPendingTaskSizeForQuarantine) {
        this.minimumWorkerThreads = minimumWorkerThreads;
        this.minimumTaskTimeForQuarantine = minimumTaskTimeForQuarantine;
        this.minimumPendingTaskSizeForQuarantine = minimumPendingTaskSizeForQuarantine;

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