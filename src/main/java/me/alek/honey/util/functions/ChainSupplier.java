package me.alek.honey.util.functions;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@FunctionalInterface
public interface ChainSupplier<T> extends Supplier<T> {

    Supplier<Object> EMPTY = new Supplier<Object>() {
        @Override
        public Object get() {
            return null;
        }
    };

    static <T> ChainSupplier<T> empty() {
        return () -> (T) EMPTY.get();
    }

    static <T> ChainSupplier<T> wrap(Supplier<T> supplier) {
        return supplier::get;
    }

    default <V> ChainSupplier<V> andThen(Function<T, V> function) {
        return () -> function.apply(ChainSupplier.this.get());
    }

    default ChainSupplier<T> thenDoIf(Predicate<T> filter, Runnable task) {
        return () -> {
            T val = ChainSupplier.this.get();
            if (filter.test(ChainSupplier.this.get())) {
                task.run();
            }
            return val;
        };
    }

    default ChainSupplier<T> thenDo(Consumer<T> function) {
        return () -> {
            T val = ChainSupplier.this.get();
            function.accept(val);
            return val;
        };
    }

    default ChainSupplier<T> thenDo(Runnable task) {
        return thenDo(v -> task.run());
    }
}
