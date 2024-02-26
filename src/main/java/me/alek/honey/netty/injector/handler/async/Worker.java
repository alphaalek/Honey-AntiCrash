package me.alek.honey.netty.injector.handler.async;

import java.util.List;
import java.util.Queue;

public interface Worker<T> {

    void submit(ChainedCallable<T> task);

    Queue<ChainedCallable<T>> getPendingTasks();

    Queue<ChainedCallable<T>> getPendingTasks(TaskSupplier<T> supplier);

    ChainedCallable<T> getCurrentTask();

    boolean relocate(TaskSupplier<T> supplier);

    boolean isQuarantined();

    long recentTaskStart();

    void destroy();

}
