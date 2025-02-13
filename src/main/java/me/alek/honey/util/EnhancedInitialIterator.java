package me.alek.honey.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Function;

public class EnhancedInitialIterator<T, I extends Iterable<T>> implements Iterable<T> {

    private final I iterable;
    private final Function<I, T> initial;

    public EnhancedInitialIterator(I iterable, Function<I, T> initial) {
        this.iterable = iterable;
        this.initial = initial;
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return new Iterator<T>() {

            private volatile boolean hasCalledInitial;
            private final LazyInit<T> initial = new LazyInit<>(() -> EnhancedInitialIterator.this.initial.apply(iterable));
            private final LazyInit<Iterator<T>> itr = new LazyInit<>(() -> {
                Iterator<T> itr = iterable.iterator();
                itr.next();
                return itr;
            });

            @Override
            public boolean hasNext() {
                if (!this.hasCalledInitial) {
                    return this.initial.get() != null;
                }

                return this.itr.get().hasNext();
            }

            @Override
            public T next() {
                if (!this.hasCalledInitial) {
                    this.hasCalledInitial = true;
                    return this.initial.get();
                }

                return this.itr.get().next();
            }
        };
    }
}
