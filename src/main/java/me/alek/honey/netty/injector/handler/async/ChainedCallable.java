package me.alek.honey.netty.injector.handler.async;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChainedCallable<T> implements Callable<T> {

    private final TaskSupplier<T> from;
    private final Callable<T> callable;
    private final Queue<Consumer<T>> listeners = new LinkedList<>();
    private volatile boolean called;

    public ChainedCallable(TaskSupplier<T> from, Callable<T> delegate) {
        this.from = from;
        this.callable = delegate;
    }

    public ChainedCallable<T> addListener(Consumer<T> listener) {
        this.listeners.add(listener);
        return this;
    }

    public ChainedCallable<T> addListener(Function<T, T> listener) {
        this.listeners.add((s) -> new ChainedCallable<>(from, () -> s).addListener(listener));
        return this;
    }

    public TaskSupplier<T> getFrom() {
        return from;
    }

    public boolean isCalled() {
        return called;
    }

    @Override
    public T call() throws Exception {
        T value = callable.call();
        synchronized (this) {
            for (Consumer<T> listener : listeners) {
                listener.accept(value);
            }
            called = true;
        }
        return value;
    }
}
