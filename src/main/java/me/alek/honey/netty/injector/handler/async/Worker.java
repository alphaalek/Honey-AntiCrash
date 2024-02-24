package me.alek.honey.netty.injector.handler.async;

import java.util.Queue;

public interface Worker<T> {

    void submit(ChainedCallable<T> task);

    Queue<ChainedCallable<T>> getPendingTasks();

    ChainedCallable<T> getCurrentTask();

    boolean shouldRelocate(TaskSupplier<T> supplier);

    boolean shouldQuarantine();

    boolean isQuarantined();

    void setQuarantined();

    boolean isActive();

    void destroy();

}
