package me.alek.honey.netty.injector.handler.async.worker;

import me.alek.honey.netty.injector.handler.async.TaskSupplier;

public interface DelegatedIsolatorWorker<T> {

    boolean isQuarantined();

    boolean relocate(TaskSupplier<T> supplier);

    default void preCall() {}

    default void postCall() {}

}
