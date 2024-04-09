package me.alek.honey.async;

public interface RunnableWorker<T> extends Worker<T> {

    void run() throws Exception;
}
