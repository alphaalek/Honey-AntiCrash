package me.alek.honey.async.worker;

import me.alek.honey.async.TaskSupplier;

public interface DelegatedIsolatorWorker<T> {

    boolean shouldQuarantine();

    boolean relocate(TaskSupplier<T> supplier);

    default void preCall() {}

    default void postCall() {}

}
