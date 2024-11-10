package me.alek.honey.util;

import java.util.Iterator;
import java.util.function.Function;

public class EnhancedInitialIterator<T, I extends Iterable<T>> implements Iterable<T> {

    private final I iterable;
    private final Function<I, LazyInit<T>> initial;

    public EnhancedInitialIterator(I iterable, Function<I, LazyInit<T>> initial) {
        this.iterable = iterable;
        this.initial = initial;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {

            private volatile boolean hasCalledInitial;
            private final LazyInit<T> initial = EnhancedInitialIterator.this.initial.apply(iterable);
            private final LazyInit<Iterator<T>> itr = new LazyInit<>(() -> {
                Iterator<T> itr = iterable.iterator();
                itr.next();
                return itr;
            });

            @Override
            public boolean hasNext() {
                if (!hasCalledInitial) {
                    return initial.get() != null;
                }

                return itr.get().hasNext();
            }

            @Override
            public T next() {
                if (!hasCalledInitial) {
                    hasCalledInitial = true;
                    return initial.get();
                }

                return itr.get().next();
            }
        };
    }
}
