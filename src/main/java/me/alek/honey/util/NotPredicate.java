package me.alek.honey.util;

import java.util.function.Predicate;

public class NotPredicate {

    public static <V> Predicate<V> not(Predicate<V> predicate) {
        return v -> !predicate.test(v);
    }
}
