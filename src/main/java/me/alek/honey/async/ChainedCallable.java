package me.alek.honey.async;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChainedCallable<T> implements Callable<T> {

    private final TaskSupplier<T> from;
    private final Callable<T> callable;
    private final Queue<Consumer<T>> listeners = new LinkedList<>();
    private final Object wait = new Object();
    private T value;
    private volatile boolean called;

    public ChainedCallable(TaskSupplier<T> from, Callable<T> delegate) {
        this.from = from;
        this.callable = delegate;
    }

    public ChainedCallable<T> addListener(Consumer<T> listener) {
        this.listeners.add(listener);
        return this;
    }

    public TaskSupplier<T> getFrom() {
        return from;
    }

    public boolean isCalled() {
        return called;
    }

    public synchronized T getValue() {
        boolean interrupted = false;
        synchronized (wait) {
            if (!called) {
                try {
                    wait.wait();
                } catch (Exception ex) {
                    interrupted = true;
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        return value;
    }

    @Override
    public T call() throws Exception {
        value = callable.call();
        synchronized (this) {
            for (Consumer<T> listener : listeners) {
                listener.accept(value);
            }
            called = true;

            synchronized (wait) {
                wait.notifyAll();
            }
        }
        return value;
    }
}
