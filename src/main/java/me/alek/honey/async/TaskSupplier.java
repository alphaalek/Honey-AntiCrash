package me.alek.honey.async;

public interface TaskSupplier<T> {

    void submitTask(ChainedCallable<T> task);

    ChainedCallable<T> getTask();
}
