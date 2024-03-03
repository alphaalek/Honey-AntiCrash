package me.alek.honey.netty.injector.handler.async;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.injector.handler.async.worker.*;
import me.alek.honey.utility.PredicateUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.*;
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

    public void sendDebug(CommandSender sender) {
        sender.sendMessage(workers.length + " available workers");
        sender.sendMessage(allWorkers.size() + " registered workers");
        for (Worker<T> worker : workers) {
            sender.sendMessage("Worker: " + worker.getPendingTasks().size() + " pending, quarantined: " + worker.isQuarantined() + ", task start: " + worker.recentTaskStart() + ", active: " + worker.isActive());
        }
    }

    private Worker<T>[] workers;
    private final Set<Worker<T>> allWorkers = new HashSet<>();
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
            running = false;
        }

        @Override
        public boolean relocate(TaskSupplier<T> supplier) {
            for (RunnableWorker<T> worker : workers) {
                if (worker.relocate(supplier)) {
                    this.worker = worker;
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
        public boolean isActive() {
            return running;
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

        Worker<T> newWorker = initNewTaskWorker(newWorkers, 0);

        this.workers = newWorkers;
        return newWorker;
    }

    private Worker<T> initNewTaskWorker(Worker<T>[] newWorkers, int index) {
        TaskWorker newWorker = new TaskWorker();
        newWorkers[index] = newWorker;
        allWorkers.add(newWorker);

        new Thread(newWorker, getTaskWorkerThreadName(index + 1)).start();

        return newWorker;
    }

    public synchronized void garbageCollect() {
        List<Worker<T>> availableWorkers = Arrays.asList(workers);
        allWorkers.addAll(availableWorkers);
        for (Worker<T> worker : allWorkers) {
            if (!availableWorkers.contains(worker)) {
                worker.destroy();
                relocateWorkerTasks(worker);
            }
        }
        allWorkers.removeIf(PredicateUtils.not(Worker::isActive));

        List<Worker<T>> pressuredWorkers = Arrays.stream(workers)
                .filter(Worker::isQuarantined)
                .collect(Collectors.toList());

        if (workers.length - pressuredWorkers.size() > minimumWorkerThreads) {
            Worker<T>[] newWorkers = new Worker[minimumWorkerThreads + pressuredWorkers.size()];
            System.arraycopy(pressuredWorkers.toArray(new Worker[0]), 0, newWorkers, workers.length - pressuredWorkers.size() - 1, pressuredWorkers.size());

            int i = 0;
            for (Worker<T> worker : workers) {
                newWorkers[i++] = worker;
                if (i == newWorkers.length || newWorkers[i] != null) break;
            }
            workers = newWorkers;
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
        return "packet-decoder-" + indexName;
    }

    public WorkerPool(int minimumWorkerThreads, double minimumTaskTimeForQuarantine, int minimumPendingTaskSizeForQuarantine) {
        this.minimumWorkerThreads = minimumWorkerThreads;
        this.minimumTaskTimeForQuarantine = minimumTaskTimeForQuarantine;
        this.minimumPendingTaskSizeForQuarantine = minimumPendingTaskSizeForQuarantine;

        this.workers = new Worker[minimumWorkerThreads];
        for (int i = 0; i < minimumWorkerThreads; i++) {
            initNewTaskWorker(workers, i);
        }

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::garbageCollect, 0, 30L, TimeUnit.SECONDS);
    }

    public void terminate() {
        for (Worker<T> worker : workers) {
            worker.destroy();
        }
    }
}