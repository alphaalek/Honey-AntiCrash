package me.alek.honey.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface ChainSupplier<T> extends Supplier<T> {

    static <T> ChainSupplier<T> wrap(Supplier<T> supplier) {
        return supplier::get;
    }

    default <V> ChainSupplier<V> andThen(Function<T, V> function) {
        return () -> function.apply(this.get());
    }

    default ChainSupplier<T> thenDo(Consumer<T> function) {
        Supplier<T> prev = this;
        return () -> {
            T val = prev.get();
            function.accept(val);
            return val;
        };
    }

    default ChainSupplier<T> thenDo(Runnable task) {
        return thenDo(v -> task.run());
    }
}
