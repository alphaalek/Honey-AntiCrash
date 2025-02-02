package me.alek.honey.async;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class ChainedCallable<T> implements Callable<T> {

    private final TaskSupplier<T> from;
    private final Callable<T> callable;
    private final Queue<Consumer<T>> listeners = new LinkedList<>();
    private final Object wait = new Object();

    private volatile T value;
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
        return this.from;
    }

    public boolean isCalled() {
        return this.called;
    }

    public T getValue() {
        synchronized (this.wait) {
            if (!this.called) {
                try {
                    this.wait.wait();
                } catch (Exception ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return this.value;
    }

    @Override
    public T call() throws Exception {
        this.called = true;
        synchronized (this.wait) {
            this.value = this.callable.call();
            for (Consumer<T> listener : this.listeners) {
                listener.accept(this.value);
            }

            this.wait.notifyAll();
        }

        return this.value;
    }
}
