package me.alek.honey.netty.injector.handler.async;

public interface TaskSupplier<T> {

    void submitTask(ChainedCallable<T> task);

    ChainedCallable<T> getTask();
}
