package me.alek.honey.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CallbackAction<T> {

    private final List<Consumer<T>> callbacks = new ArrayList<>();

    public void addAction(Consumer<T> callback) {
        callbacks.add(callback);
    }

    public void call(T val) {
        for (Consumer<T> callback : callbacks) {
            callback.accept(val);
        }
    }

    public boolean isEmpty() {
        return callbacks.isEmpty();
    }
}
