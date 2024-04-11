package me.alek.honey.util;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class CallbackAction<T> {

    private final List<Consumer<T>> callbacks = new LinkedList<>();

    public synchronized void addAction(Consumer<T> callback) {
        callbacks.add(callback);
    }

    public synchronized void call(T val) {
        if (callbacks.isEmpty()) {
            return;
        }

        for (Consumer<T> callback : callbacks) {
            callback.accept(val);
        }
    }

    public synchronized void call() {
        call(null);
    }

    public boolean isEmpty() {
        return callbacks.isEmpty();
    }
}
