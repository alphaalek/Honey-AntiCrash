package me.alek.honey.netty.injector.handler.async;

public interface RunnableWorker<T> extends Worker<T> {

    void run() throws Exception;
}
