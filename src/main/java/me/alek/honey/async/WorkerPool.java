package me.alek.honey.async;

import me.alek.honey.async.worker.DelegatedIsolatorWorker;
import me.alek.honey.async.worker.HeavyPacketIsolatorWorker;
import me.alek.honey.async.worker.HighPendingIsolatorWorker;
import me.alek.honey.async.worker.IsolatorWorkerWrapper;
import me.alek.honey.configuration.ConfigurationOptions;
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
        sender.sendMessage(Arrays.toString(workers));
        getWorkerForTask();
        for (Worker<T> worker : workers) {
            sender.sendMessage("Worker:"
                    + "  pending: " + worker.getPendingTasks().size()
                    + ", quarantined: " + worker.isQuarantined()
                    + ", task start: " + worker.recentTaskStart()
                    + ", active: " + worker.isActive()
                    + ", count: " + worker.supplierCount());
        }
    }

    private Worker<T>[] workers;
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

    private List<Worker<T>> initializeDelegatedWorkers() {
        List<Worker<T>> workers = new ArrayList<>();
        for (Class<DelegatedIsolatorWorker<?>> workerClass : WORKERS_CLASSES) {
            try {
                @SuppressWarnings("unchecked")
                Worker<T> workerWrapper = IsolatorWorkerWrapper.class.getDeclaredConstructor(WorkerPool.class, Class.class)
                        .newInstance(this, workerClass);

                workers.add(workerWrapper);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return workers;
    }

    public class TaskWorker implements Worker<T> {

        private boolean running = true;

        private final String name;
        private final List<Worker<T>> workers = initializeDelegatedWorkers();
        private Worker<T> worker = workers.get(0);

        public TaskWorker(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    worker.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        @Override
        public boolean relocate(TaskSupplier<T> supplier) {
            for (Worker<T> worker : workers) {
                if (worker.relocate(supplier)) {
                    this.worker = worker;
                    return true;
                }
            }
            return false;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void submit(TaskSupplier<T> supplier) {
            worker.submit(supplier);
        }

        @Override
        public void submit(ChainedCallable<T> callable) {
            worker.submit(callable);
        }

        @Override
        public void incrementSupplierCount() {
            worker.incrementSupplierCount();
        }

        @Override
        public void decrementSupplierCount() {
            worker.decrementSupplierCount();
        }

        @Override
        public int supplierCount() {
            return worker.supplierCount();
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

    public synchronized Worker<T> submit(TaskSupplier<T> supplier) {
        Worker<T> worker = getWorkerForTaskAndRegisterSupplier();
        worker.submit(supplier);

        return worker;
    }

    public synchronized Worker<T> getWorkerForTaskAndRegisterSupplier() {
        Worker<T> worker = getWorkerForTask();
        worker.incrementSupplierCount();

        return worker;
    }

    public synchronized Worker<T> getWorkerForTask() {
        if (workers.length == 0) {
            return createAndPushNewTaskWorker();
        }

        for (int i = 0; i < workers.length - 1; i++) {
            Worker<T> worker = workers[i];
            pushTaskWorker(worker, i + 1);
        }

        if (workers[0].isQuarantined()) {
            return createAndPushNewTaskWorker();
        }

        return workers[0];
    }

    public synchronized void pushTaskWorker(Worker<T> worker, int start) {
        for (int i = start; i < workers.length; i++) {
            Worker<T> otherWorker = workers[i];
            /*
            if (otherWorker == null) {
                Bukkit.getLogger().info("Found null worker! " + i);
                otherWorker = initNewTaskWorker(workers, i);
            }
            */

            if (worker.isQuarantined()
                    || worker.getPendingTasks().size() > otherWorker.getPendingTasks().size()
                    || worker.supplierCount() > otherWorker.supplierCount()) {

                workers[i] = worker;
                workers[i - 1] = otherWorker;
                continue;
            }

            break;
        }
    }

    public synchronized Worker<T> createAndReplaceNewTaskWorker() {
        if (workers.length == 0) {
            return createAndPushNewTaskWorker();
        }

        return initNewTaskWorker(workers, 0);
    }

    public synchronized Worker<T> createAndPushNewTaskWorker() {
        Worker<T>[] newWorkers = new Worker[workers.length + 1];
        System.arraycopy(workers, 0, newWorkers, 1, workers.length);

        Worker<T> newWorker = initNewTaskWorker(newWorkers, 0);

        this.workers = newWorkers;
        return newWorker;
    }

    private synchronized Worker<T> initNewTaskWorker(Worker<T>[] newWorkers, int index) {
        Worker<T> worker = initNewTaskWorker();
        newWorkers[index] = worker;

        return worker;
    }

    private synchronized Worker<T> initNewTaskWorker() {
        TaskWorker newWorker = new TaskWorker(getTaskWorkerThreadName());
        new Thread(newWorker, newWorker.name()).start();

        return newWorker;
    }

    public synchronized void garbageCollect() {
        List<Worker<T>> quarantinedWorkers = Arrays.stream(workers)
                .filter(Worker::isQuarantined)
                .collect(Collectors.toList());

        List<Worker<T>> otherWorkers = new ArrayList<>(Arrays.asList(workers));
        otherWorkers.removeAll(quarantinedWorkers);

        if (workers.length - quarantinedWorkers.size() > minimumWorkerThreads) {
            try {
                @SuppressWarnings("unchecked")
                Worker<T>[] newWorkers = new Worker[minimumWorkerThreads + quarantinedWorkers.size()];
                if (!quarantinedWorkers.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Worker<T>[] quarantinedWorkersArray = quarantinedWorkers.toArray((Worker<T>[]) new Worker[0]);
                    System.arraycopy(quarantinedWorkersArray, 0, newWorkers, minimumWorkerThreads, quarantinedWorkers.size());
                }

                Iterator<Worker<T>> otherWorkerIterator = otherWorkers.iterator();
                int i = 0;
                while (i < minimumWorkerThreads && otherWorkerIterator.hasNext()) {
                    Worker<T> worker = otherWorkerIterator.next();
                    otherWorkerIterator.remove();
                    newWorkers[i++] = worker;
                }
                if (Arrays.stream(newWorkers).anyMatch(Objects::isNull)) {
                    Bukkit.getLogger().info("Aborting GC (3), workers: " + Arrays.asList(workers) + " size: " + workers.length + " quarantined: " + quarantinedWorkers.size() + " min: " + minimumWorkerThreads + " other: " + otherWorkers.size());
                    return;
                }
                workers = newWorkers;

                // destroy remaining workers
                for (Worker<T> worker : otherWorkers) {
                    worker.destroy();
                }
                Bukkit.getLogger().info("Post GC (1), new: " + Arrays.asList(newWorkers) + " active: " + Arrays.stream(newWorkers).filter(Worker::isActive).count() + " size: " + newWorkers.length + " quarantined: " + quarantinedWorkers.size() + " min: " + minimumWorkerThreads + " other: " + otherWorkers.size());

            } catch (Exception ex) {
                Bukkit.getLogger().info("GC error (2), workers: " + Arrays.asList(workers) + " size: " + workers.length + " quarantined: " + quarantinedWorkers.size() + " min: " + minimumWorkerThreads + " other: " + otherWorkers.size());
                ex.printStackTrace();
            }
        }
    }

    public Worker<T> relocateWorkerTasks(Worker<T> from) {
        from.decrementSupplierCount();
        return relocateWorkerTasks(from.getPendingTasks());
    }

    public Worker<T> relocateWorkerTasks(TaskSupplier<T> supplier, Worker<T> from) {
        from.decrementSupplierCount();
        return relocateWorkerTasks(from.getPendingTasks(supplier));
    }

    private Worker<T> relocateWorkerTasks(Queue<ChainedCallable<T>> tasks) {
        Worker<T> worker = getWorkerForTaskAndRegisterSupplier();
        for (ChainedCallable<T> task : tasks) {
            worker.submit(task);
        }
        return worker;
    }

    public synchronized String getTaskWorkerThreadName() {
        Set<Integer> indices = new HashSet<>();
        for (Worker<T> worker : workers) {
            if (worker == null || worker.name() == null) {
                continue;
            }

            int index = Integer.parseInt(worker.name().split("-")[3]);
            indices.add(index);
        }

        for (int i = 0; ; i++) {
            if (!indices.contains(i)) {
                return "honey-packet-decoder-" + i;
            }
        }
    }

    public WorkerPool(int minimumWorkerThreads, double minimumTaskTimeForQuarantine, int minimumPendingTaskSizeForQuarantine) {
        this.minimumWorkerThreads = Math.max(1, minimumWorkerThreads);
        this.minimumTaskTimeForQuarantine = Math.max(5, minimumTaskTimeForQuarantine);
        this.minimumPendingTaskSizeForQuarantine = Math.max(5, minimumPendingTaskSizeForQuarantine);

        this.workers = new Worker[minimumWorkerThreads];
        for (int i = 0; i < minimumWorkerThreads; i++) {
            initNewTaskWorker(workers, i);
        }

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::garbageCollect, 0, 30L, TimeUnit.SECONDS);
    }

    public void terminate() {
        if (workers == null) return;

        for (Worker<T> worker : workers) {
            if (worker != null) {
                worker.destroy();
            }
        }
    }
}