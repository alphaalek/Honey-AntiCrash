package me.alek.honey.util;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Callback {

    public interface Action extends Predicate<Object[]> {

        static <K> Action from(Consumer<K> val) {
            return new Action() {
                @Override
                public boolean test(Object[] objects) {
                    return objects.length == 1;
                }

                @Override
                public void call(Object... data) {
                    val.accept((K) data[0]);
                }
            };
        }

        static <K, V> Action from(BiConsumer<K, V> val) {
            return new Action() {
                @Override
                public boolean test(Object[] objects) {
                    return objects.length == 2;
                }

                @Override
                public void call(Object... data) {
                    val.accept((K) data[0], (V) data[1]);
                }
            };
        }

        void call(Object... data);
    }

    private final List<Action> callbacks = new LinkedList<>();

    public void addAction(Action callback) {
        callbacks.add(callback);
    }

    public void call(Object... data) {
        if (isEmpty()) {
            return;
        }

        for (Action callback : callbacks) {
            if (callback.test(data)) {
                callback.call(data);
            }
        }
    }

    public boolean isEmpty() {
        return callbacks.isEmpty();
    }
}
