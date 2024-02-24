package me.alek.honey.netty.injector.handler.async;

import java.util.Queue;

public interface Worker<T> {

    void submit(ChainedCallable<T> task);

    Queue<ChainedCallable<T>> getPendingTasks();

    boolean shouldRelocate(TaskSupplier<T> supplier);

    boolean isQuarantined();

    ChainedCallable<T> getCurrentTask();

    void setQuarantined();

}
