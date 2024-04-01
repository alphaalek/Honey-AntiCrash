package me.alek.honey.netty.injector.handler.async;

import java.util.Queue;

public interface Worker<T> {

    String name();

    void submit(TaskSupplier<T> supplier);

    void submit(ChainedCallable<T> callable);

    void incrementSupplierCount();

    void decrementSupplierCount();

    int supplierCount();

    Queue<ChainedCallable<T>> getPendingTasks();

    Queue<ChainedCallable<T>> getPendingTasks(TaskSupplier<T> supplier);

    ChainedCallable<T> getCurrentTask();

    boolean relocate(TaskSupplier<T> supplier);

    boolean isQuarantined();

    long recentTaskStart();

    boolean isActive();

    void destroy();
}
