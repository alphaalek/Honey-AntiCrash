package me.alek.honey.util;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Alternately<V> {

    public enum Type {
        CURRENT, OTHER
    }

    private final V v1;
    private final V v2;

    public int current = 0;

    public Alternately(Supplier<V> get) {
        this(get.get(), get.get());
    }

    public Alternately(V v1, V v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    public void doIf(Predicate<V> onlyIf, Consumer<V> andThen) {
        if (onlyIf.test(v1)) {
            andThen.accept(v1);
        } else if (onlyIf.test(v2)) {
            andThen.accept(v2);
        }
    }

    public <T> T applyFor(Comparator<V> comparator, Predicate<V> onlyIf, Function<V, T> andThen) {
        int expr = comparator.compare(v1, v2);

        if ((onlyIf == null || onlyIf.test(v1))
                && expr < 1) { // LESS, EQUAL
            return andThen.apply(v1);
        }

        if ((onlyIf == null ||onlyIf.test(v2))
                && expr > -1) { // GREATER, EQUAL
            return andThen.apply(v2);
        }

        return null;
    }

    public <T> T applyFor(Comparator<V> comparator, Function<V, T> andThen) {
        return applyFor(comparator, null, andThen);
    }

    public boolean doFor(Comparator<V> comparator, Predicate<V> onlyIf, Consumer<V> andThen, boolean both) {
        int expr = comparator.compare(v1, v2);
        boolean did = false;

        if ((onlyIf == null || onlyIf.test(v1))
                && expr < (both ? 1 : 0)) { // LESS, EQUAL
            andThen.accept(v1);
            did = true;
        }

        if ((onlyIf == null ||onlyIf.test(v2))
                && expr > (both ? -1 : 0)) { // GREATER, EQUAL
            andThen.accept(v2);
            did = true;
        }

        return did;
    }

    public void doForWhile(Comparator<V> comparator, Predicate<V> onlyIf, Consumer<V> andThen, boolean both) {
        for (;;) {
            if (!doFor(comparator, onlyIf, andThen, both)) {
                return;
            }
        }
    }

    public V get(int i) {
        return i == 0 ? v1 : v2;
    }

    public V get(Type index) {
        return index == Type.CURRENT ? getCurrent() : getOther();
    }

    public V getCurrent() {
        return get(current);
    }

    public V getOther() {
        return get(other(current));
    }

    private int other(int i) {
        return i == 0 ? 1 : 0;
    }

    public void switchInternal() {
        current = other(current);
    }
}
